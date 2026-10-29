package org.javaboy.vhr;

import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.EmployeeEcService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses the same isolated schema fixture and exact migrations as transfer tests. */
class EmployeeEcTests {
    EmployeeTransferTests fixture;
    EmployeeEcService service;
    JdbcTemplate jdbc;
    Hr actor;
    @BeforeEach void setup() throws Exception {
        fixture=new EmployeeTransferTests(); fixture.setup();
        service=fixture.context.getBean(EmployeeEcService.class); jdbc=fixture.jdbc; actor=fixture.actor;
    }
    @AfterEach void close() { if(fixture!=null)fixture.close(); }
    String key() {return UUID.randomUUID().toString();}
    LocalDate today() {return LocalDate.now(ZoneId.of("Asia/Shanghai"));}
    EcRuleCommand rule(String key,Integer version,boolean enabled) {return new EcRuleCommand(version,"优秀贡献",0,new BigDecimal("100.00"),"项目贡献",enabled,key);}
    int createRule() {return service.saveRule(null,rule(key(),null,true),actor);}
    EcCommand command(int rule,String key) {return new EcCommand(10,rule,0,today(),new BigDecimal("123.45"),"项目按期交付","备注",key);}
    EcRecord record() {return (EcRecord)service.records(1,10,null,null,null,null,null).getData().getFirst();}
    int count(String table) {return fixture.count(table);}

    @Test void recordsSnapshotsAndExactDecimalWithoutChangingEmployeeOrLegacyPoints() {
        int rule=createRule();
        int id=service.register(command(rule,key()),actor);
        service.saveRule(rule,new EcRuleCommand(0,"新规则名",1,new BigDecimal("200"),"新说明",true,key()),actor);
        jdbc.update("UPDATE employee SET name='新姓名' WHERE id=10");
        EcRecord saved=record();
        assertEquals(id,saved.getId());assertEquals("测试员工",saved.getEmployeeName());
        assertEquals("优秀贡献",saved.getRuleName());assertEquals(0,saved.getEcType());
        assertEquals(new BigDecimal("123.45"),saved.getAmount());assertEquals(new BigDecimal("100.00"),saved.getDefaultAmount());
        assertEquals("研发部",saved.getDepartmentName());assertNull(saved.getEcPoint());
        assertEquals(1,jdbc.queryForObject("SELECT departmentId FROM employee WHERE id=10",Integer.class));
        TransferOperationLog log=(TransferOperationLog)fixture.service.logs(1,10,"00000010",null,null,null).getData().getFirst();
        assertTrue(log.getDetails().contains("123.45"));assertTrue(log.getDetails().contains("项目按期交付"));
        assertEquals(3,count("oplog"));
    }
    @Test void ruleAndRecordRetriesAreIdempotentAndRejectDifferentPayloadOrActor() {
        String ruleKey=key();EcRuleCommand rule=rule(ruleKey,null,true);
        int ruleId=service.saveRule(null,rule,actor);
        assertEquals(ruleId,service.saveRule(null,rule,actor));assertEquals(1,count("ec_rule"));
        String registerKey=key();EcCommand command=command(ruleId,registerKey);
        int id=service.register(command,actor);
        assertEquals(id,service.register(command,actor));assertEquals(1,count("employeeec"));assertEquals(2,count("oplog"));
        assertThrows(IllegalArgumentException.class,()->service.register(new EcCommand(10,ruleId,0,today(),new BigDecimal("9"),"项目按期交付","备注",registerKey),actor));
        Hr other=new Hr();other.setId(8);
        assertThrows(IllegalArgumentException.class,()->service.register(command,other));
        EcRuleCommand update=rule(key(),0,false);
        service.saveRule(ruleId,update,actor);service.saveRule(ruleId,update,actor);
        assertEquals(1,((EcRule)service.rules(1,10,null,null,null).getData().getFirst()).getVersion());
        // Retrying a committed registration still succeeds after the rule has been disabled.
        assertEquals(id,service.register(command,actor));
    }
    @Test void staleOrDisabledRulesAndInvalidAmountsDatesEmployeesAreRejected() {
        int rule=createRule();
        for(BigDecimal amount:List.of(new BigDecimal("-1"),new BigDecimal("0.001"),new BigDecimal("100000000"))) {
            assertThrows(IllegalArgumentException.class,()->service.register(new EcCommand(10,rule,0,today(),amount,"原因","",key()),actor));
        }
        assertThrows(IllegalArgumentException.class,()->service.register(new EcCommand(11,rule,0,today(),BigDecimal.ZERO,"原因","",key()),actor));
        assertThrows(IllegalArgumentException.class,()->service.register(new EcCommand(10,rule,0,today().plusDays(1),BigDecimal.ZERO,"原因","",key()),actor));
        assertThrows(IllegalArgumentException.class,()->service.register(new EcCommand(10,rule,0,today(),BigDecimal.ZERO," ","",key()),actor));
        service.saveRule(rule,rule(key(),0,true),actor);
        assertThrows(IllegalArgumentException.class,()->service.register(command(rule,key()),actor));
        assertThrows(IllegalArgumentException.class,()->service.saveRule(rule,rule(key(),0,false),actor));
        service.saveRule(rule,rule(key(),1,false),actor);
        assertThrows(IllegalArgumentException.class,()->service.register(new EcCommand(10,rule,2,today(),BigDecimal.ZERO,"原因","",key()),actor));
        assertEquals(0,count("employeeec"));
    }
    @Test void registrationAndRuleUpdateRollBackWhenAuditInsertFails() {
        int rule=createRule();
        jdbc.execute("ALTER TABLE oplog ADD CONSTRAINT reject_new_log CHECK(ecAction='RULE_CREATE')");
        assertThrows(RuntimeException.class,()->service.register(command(rule,key()),actor));
        assertEquals(0,count("employeeec"));
        assertThrows(RuntimeException.class,()->service.saveRule(rule,rule(key(),0,false),actor));
        EcRule unchanged=(EcRule)service.rules(1,10,null,null,null).getData().getFirst();
        assertTrue(unchanged.getEnabled());assertEquals(0,unchanged.getVersion());assertEquals(1,count("oplog"));
    }
    @Test void cancellationPreservesRecordAndRetriesDoNotAddLogs() {
        int id=service.register(command(createRule(),key()),actor);
        EcCancelCommand cancel=new EcCancelCommand("登记错误",key());
        assertEquals(id,service.cancel(id,cancel,actor));assertEquals(id,service.cancel(id,cancel,actor));
        EcRecord saved=record();assertEquals("CANCELLED",saved.getStatus());assertEquals("登记错误",saved.getCancelReason());
        assertEquals(7,saved.getCancelledBy());assertNotNull(saved.getCancelledAt());
        assertEquals("项目按期交付",saved.getEcReason());assertEquals(1,count("employeeec"));assertEquals(3,count("oplog"));
        assertThrows(IllegalArgumentException.class,()->service.cancel(id,new EcCancelCommand("再次撤销",key()),actor));
    }
    @Test void auditFailureRollsBackCancellation() {
        int id=service.register(command(createRule(),key()),actor);
        jdbc.execute("ALTER TABLE oplog ADD CONSTRAINT reject_cancel CHECK(ecAction <> 'EC_CANCEL')");
        assertThrows(RuntimeException.class,()->service.cancel(id,new EcCancelCommand("错误",key()),actor));
        assertEquals("ACTIVE",record().getStatus());assertNull(record().getCancelledAt());assertEquals(2,count("oplog"));
    }
    @Test void concurrentCancellationWritesExactlyOneCancellationLog() throws Exception {
        int id=service.register(command(createRule(),key()),actor);
        CountDownLatch start=new CountDownLatch(1);
        try(ExecutorService executor=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> task=()->{start.await();try{service.cancel(id,new EcCancelCommand("重复登记",key()),actor);return true;}catch(IllegalArgumentException e){return false;}};
            Future<Boolean> a=executor.submit(task),b=executor.submit(task);start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
        }
        assertEquals(3,count("oplog"));assertEquals("CANCELLED",record().getStatus());
    }
    @Test void filtersPaginationAndLegacyScoresRemainReadable() {
        int rule=createRule();service.register(command(rule,key()),actor);
        jdbc.update("INSERT INTO employeeec(eid,ecDate,ecType,ecReason,ecPoint) VALUES(10,'2020-01-01',1,'历史处罚',5)");
        EcRecord legacy=(EcRecord)service.records(1,10,"00000010",1,"ACTIVE",LocalDate.of(2020,1,1),LocalDate.of(2020,1,1)).getData().getFirst();
        assertEquals(5,legacy.getEcPoint());assertNull(legacy.getAmount());assertNull(legacy.getRuleName());assertNull(legacy.getOperatorName());
        assertEquals(1L,service.records(1,10,"测试",0,"ACTIVE",today(),today()).getTotal());
        assertEquals(2L,service.records(2,1,null,null,null,null,null).getTotal());
        assertEquals(1,service.records(2,1,null,null,null,null,null).getData().size());
        assertEquals(0L,service.records(1,10,"' OR 1=1 --",null,null,null,null).getTotal());
        assertEquals(1L,service.rules(1,10,"贡献",0,true).getTotal());
        assertThrows(IllegalArgumentException.class,()->service.records(0,10,null,null,null,null,null));
        assertThrows(IllegalArgumentException.class,()->service.rules(1,10,null,99,null));
        assertThrows(IllegalArgumentException.class,()->service.records(1,10,null,null,"BAD",null,null));
        assertThrows(IllegalArgumentException.class,()->service.records(1,10,null,null,null,today(),today().minusDays(1)));
    }
}
