package org.javaboy.vhr;

import org.h2.jdbcx.JdbcDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.javaboy.vhr.mapper.TransferMapper;
import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.EmployeeTransferService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MyBatis SQL + Spring transactions against an isolated H2 database. */
class EmployeeTransferTests {
    AnnotationConfigApplicationContext context;
    JdbcTemplate jdbc;
    EmployeeTransferService service;
    Hr actor;

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean DataSource dataSource() {
            JdbcDataSource source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
            return source;
        }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean SqlSessionFactoryBean sqlSessionFactory(DataSource source) {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setMapperLocations(new ClassPathResource("org/javaboy/vhr/mapper/TransferMapper.xml"));
            return factory;
        }
        @Bean TransferMapper transferMapper(SqlSessionFactory factory) { return new SqlSessionTemplate(factory).getMapper(TransferMapper.class); }
        @Bean EmployeeTransferService service(TransferMapper mapper) { return new EmployeeTransferService(mapper); }
    }

    @BeforeEach void setup() throws Exception {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        DataSource source = context.getBean(DataSource.class);
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE department(id INT PRIMARY KEY, name VARCHAR(64), enabled BOOLEAN, parentId INT)");
        jdbc.execute("CREATE TABLE position(id INT PRIMARY KEY, name VARCHAR(64), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE hr(id INT PRIMARY KEY, name VARCHAR(64))");
        jdbc.execute("CREATE TABLE employee(id INT PRIMARY KEY, name VARCHAR(64), workID VARCHAR(32), departmentId INT, posId INT, workState VARCHAR(16))");
        jdbc.execute("CREATE TABLE employeeremove(id INT AUTO_INCREMENT PRIMARY KEY, eid INT, afterDepId INT, afterJobId INT, removeDate DATE, reason VARCHAR(255), remark VARCHAR(255), FOREIGN KEY(eid) REFERENCES employee(id))");
        jdbc.execute("CREATE TABLE oplog(id INT AUTO_INCREMENT PRIMARY KEY, addDate DATE, operate VARCHAR(255), hrid INT, FOREIGN KEY(hrid) REFERENCES hr(id))");
        // Execute the exact new migration, rather than a hand-maintained final schema.
        try (Connection connection = source.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V3__employee_transfer_audit.sql"));
        }
        jdbc.update("INSERT INTO department VALUES(1,'研发部',TRUE,0),(2,'产品部',TRUE,0),(3,'支持部',TRUE,0),(4,'已停用',FALSE,0)");
        jdbc.update("INSERT INTO position VALUES(1,'工程师',TRUE),(2,'产品经理',TRUE),(3,'已停用',FALSE)");
        jdbc.update("INSERT INTO employee VALUES(10,'测试员工','00000010',1,1,'在职'),(11,'离职员工','00000011',1,1,'离职')");
        jdbc.update("INSERT INTO hr VALUES(7,'人事操作员')");
        actor = new Hr(); actor.setId(7); actor.setName("人事操作员");
        service = context.getBean(EmployeeTransferService.class);
    }
    @AfterEach void close() { if (jdbc != null) jdbc.execute("SHUTDOWN"); if (context != null) context.close(); }
    TransferCommand command(String id) { return new TransferCommand(10,1,1,2,2,"组织调整","立即生效",id); }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    @Test void updatesEmployeeAndPersistsSnapshotsWithOneAuditLog() {
        TransferRecord result = service.transfer(command(UUID.randomUUID().toString()), actor);
        assertEquals(2, jdbc.queryForObject("SELECT departmentId FROM employee WHERE id=10", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT posId FROM employee WHERE id=10", Integer.class));
        assertEquals(1,count("employeeremove")); assertEquals(1,count("oplog"));
        assertEquals(7, result.getOperatorId()); assertEquals("研发部", result.getBeforeDepName());
        assertNotNull(result.getCreatedAt());
        jdbc.update("UPDATE department SET name='改名后的部门' WHERE id=1");
        TransferRecord history = (TransferRecord) service.transfers(1,10,"测试",1,null,null).getData().getFirst();
        assertEquals("研发部",history.getBeforeDepName());
        assertEquals("产品部",history.getAfterDepName());
        TransferOperationLog log = (TransferOperationLog) service.logs(1,10,"00000010","人事",null,null).getData().getFirst();
        assertEquals(result.getId(),log.getTransferId()); assertEquals("组织调整",log.getReason());
        assertEquals("人事操作员",log.getOperatorName());
    }
    @Test void auditFailureRollsBackEmployeeAndTransferRecord() {
        jdbc.execute("ALTER TABLE oplog ADD CONSTRAINT reject_test_log CHECK (operate = 'reject')");
        assertThrows(RuntimeException.class, () -> service.transfer(command(UUID.randomUUID().toString()), actor));
        assertEquals(1,jdbc.queryForObject("SELECT departmentId FROM employee WHERE id=10",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT posId FROM employee WHERE id=10",Integer.class));
        assertEquals(0,count("employeeremove")); assertEquals(0,count("oplog"));
    }
    @Test void identicalRetryReturnsExistingRecordAndChangedPayloadIsRejected() {
        String key = UUID.randomUUID().toString();
        TransferRecord first = service.transfer(command(key),actor);
        TransferRecord retry = service.transfer(command(key),actor);
        assertEquals(first.getId(),retry.getId()); assertEquals(1,count("oplog"));
        TransferCommand changed = new TransferCommand(10,1,1,3,2,"组织调整","立即生效",key);
        assertThrows(IllegalArgumentException.class, () -> service.transfer(changed,actor));
        assertEquals(1,count("employeeremove"));
    }
    @Test void invalidAndStaleTransfersDoNotWriteAnything() {
        String key = UUID.randomUUID().toString();
        List<TransferCommand> invalid = List.of(
            new TransferCommand(10,1,1,1,1,"相同岗位","",key),
            new TransferCommand(10,2,1,2,2,"过期档案","",key),
            new TransferCommand(10,1,1,4,2,"停用部门","",key),
            new TransferCommand(10,1,1,2,3,"停用职位","",key),
            new TransferCommand(10,1,1,99,2,"不存在部门","",key),
            new TransferCommand(11,1,1,2,2,"离职员工","",key),
            new TransferCommand(10,1,1,2,2,"   ","",key),
            new TransferCommand(10,1,1,2,2,"x".repeat(256),"",key),
            new TransferCommand(10,1,1,2,2,"原因","","bad-key")
        );
        invalid.forEach(value -> assertThrows(IllegalArgumentException.class, () -> service.transfer(value,actor)));
        assertEquals(0,count("employeeremove")); assertEquals(0,count("oplog"));
    }
    @Test void concurrentTransfersRejectTheStaleAssignment() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> first = () -> {start.await(); try { service.transfer(command(UUID.randomUUID().toString()),actor); return true; } catch (IllegalArgumentException e) {return false;} };
            Callable<Boolean> second = () -> {start.await(); try { service.transfer(new TransferCommand(10,1,1,3,2,"另一次调动","",UUID.randomUUID().toString()),actor); return true; } catch (IllegalArgumentException e) {return false;} };
            Future<Boolean> a = executor.submit(first), b = executor.submit(second); start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,count("employeeremove")); assertEquals(1,count("oplog"));
    }
    @Test void filtersUseInclusiveEndDateAndPreserveLegacyRecords() {
        TransferRecord result = service.transfer(command(UUID.randomUUID().toString()),actor);
        LocalDate day = result.getRemoveDate();
        assertEquals(1L,service.transfers(1,10,"测试",2,day,day).getTotal());
        assertEquals(0L,service.transfers(1,10,"' OR 1=1 --",null,null,null).getTotal());
        assertEquals(0L,service.logs(1,10,null,null,day.plusDays(1),null).getTotal());
        jdbc.update("INSERT INTO employeeremove(eid,afterDepId,afterJobId,removeDate,reason) VALUES(10,1,1,'2020-01-01','历史调动')");
        TransferRecord legacy = (TransferRecord)service.transfers(1,10,null,null,LocalDate.of(2020,1,1),LocalDate.of(2020,1,1)).getData().getFirst();
        assertEquals("测试员工",legacy.getEmployeeName()); assertNull(legacy.getBeforeDepName()); assertNull(legacy.getOperatorName());
        assertThrows(IllegalArgumentException.class, () -> service.logs(0,10,null,null,null,null));
        assertThrows(IllegalArgumentException.class, () -> service.logs(1,1000,null,null,null,null));
        assertThrows(IllegalArgumentException.class, () -> service.logs(1,10,null,null,day.plusDays(1),day));
    }
    @Test void employeeLookupReturnsOnlyActiveStaffAndMinimalAssignmentData() {
        assertEquals(1,service.employees("员工").size());
        TransferEmployee option = service.employees("00000010").getFirst();
        assertEquals("研发部",option.getDepartmentName()); assertEquals(1,option.getPositionId());
        assertEquals(4,((List<?>)service.options().get("departments")).size());
    }
}
