package org.javaboy.vhr;
import org.javaboy.vhr.controller.personnel.EmployeeEcController;
import org.javaboy.vhr.controller.system.basic.EcRuleController;
import org.javaboy.vhr.exception.TransferExceptionHandler;
import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.EmployeeEcService;
import org.javaboy.vhr.service.TransferAccessService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EcControllerTests {
    EmployeeEcService service;
    TransferAccessService access;
    MockMvc mvc;
    Hr actor;
    final String record = """
        {"employeeId":10,"ruleId":1,"expectedRuleVersion":2,"date":"2026-10-01",
         "amount":123.45,"reason":"贡献","remark":"","requestId":"12345678-1234-1234-1234-123456789012"}
        """;
    final String rule = """
        {"version":2,"name":"奖励规则","type":0,"amount":100,"description":"说明","enabled":true,
         "requestId":"12345678-1234-1234-1234-123456789012"}
        """;
    @BeforeEach void setup() {
        service=mock(EmployeeEcService.class);access=mock(TransferAccessService.class);actor=new Hr();actor.setId(7);
        when(access.requireMenu(anyString())).thenReturn(actor);
        mvc=MockMvcBuilders.standaloneSetup(new EmployeeEcController(service,access),new EcRuleController(service,access))
                .setControllerAdvice(new TransferExceptionHandler()).build();
    }
    @Test void recordJsonBindsExactDecimalDateAndServerActor() throws Exception {
        when(service.register(any(),same(actor))).thenReturn(42);
        mvc.perform(post("/personnel/ec/").contentType(MediaType.APPLICATION_JSON).content(record))
                .andExpect(status().isOk()).andExpect(jsonPath("$.obj").value(42));
        verify(service).register(argThat(c->c.expectedRuleVersion()==2 && c.date().equals(LocalDate.of(2026,10,1))
                && c.amount().equals(new BigDecimal("123.45"))),same(actor));
        verify(access).requireMenu("/per/ec");
    }
    @Test void ruleAndCancelEndpointsRequireTheirOwnMenuAndHaveNoDelete() throws Exception {
        mvc.perform(put("/system/basic/ec/1").contentType(MediaType.APPLICATION_JSON).content(rule)).andExpect(status().isOk());
        verify(access).requireMenu("/sys/basic");
        verify(service).saveRule(eq(1),argThat(c->c.version()==2 && c.enabled()),same(actor));
        mvc.perform(post("/personnel/ec/42/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"重复登记\",\"requestId\":\"12345678-1234-1234-1234-123456789012\"}")).andExpect(status().isOk());
        verify(service).cancel(eq(42),argThat(c->"重复登记".equals(c.reason())),same(actor));
        mvc.perform(delete("/personnel/ec/42")).andExpect(status().isNotFound());
        mvc.perform(delete("/system/basic/ec/1")).andExpect(status().isMethodNotAllowed());
    }
    @Test void unauthorizedRequestsNeverReadOrMutateServiceData() throws Exception {
        when(access.requireMenu(anyString())).thenThrow(new AccessDeniedException("没有权限"));
        for(String path:List.of("/personnel/ec/","/personnel/ec/employees","/personnel/ec/rules","/system/basic/ec/"))
            mvc.perform(get(path)).andExpect(status().isForbidden());
        mvc.perform(post("/personnel/ec/").contentType(MediaType.APPLICATION_JSON).content(record)).andExpect(status().isForbidden());
        mvc.perform(post("/system/basic/ec/").contentType(MediaType.APPLICATION_JSON).content(rule)).andExpect(status().isForbidden());
        mvc.perform(put("/system/basic/ec/1").contentType(MediaType.APPLICATION_JSON).content(rule)).andExpect(status().isForbidden());
        mvc.perform(post("/personnel/ec/1/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"撤销\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void ruleLookupReturnsOnlyEnabledRulesAndValidationIsHttp400() throws Exception {
        RespPageBean page=new RespPageBean();page.setTotal(0L);page.setData(List.of());
        when(service.rules(anyInt(),anyInt(),any(),any(),any())).thenReturn(page);
        mvc.perform(get("/personnel/ec/rules").param("keyword","贡献")).andExpect(status().isOk());
        verify(service).rules(1,30,"贡献",null,true);
        when(service.register(any(),same(actor))).thenThrow(new IllegalArgumentException("规则已停用"));
        mvc.perform(post("/personnel/ec/").contentType(MediaType.APPLICATION_JSON).content(record))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("规则已停用"));
        mvc.perform(get("/personnel/ec/").param("startDate","invalid")).andExpect(status().isBadRequest());
    }
}
