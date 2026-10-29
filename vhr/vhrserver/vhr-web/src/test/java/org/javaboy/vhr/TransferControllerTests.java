package org.javaboy.vhr;

import org.javaboy.vhr.controller.personnel.EmployeeTransferController;
import org.javaboy.vhr.controller.system.TransferLogController;
import org.javaboy.vhr.exception.TransferExceptionHandler;
import org.javaboy.vhr.exception.GlobalExceptionHandler;
import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.EmployeeTransferService;
import org.javaboy.vhr.service.TransferAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP binding and authorization checks without starting the application or contacting its database. */
class TransferControllerTests {
    EmployeeTransferService service;
    TransferAccessService access;
    MockMvc mvc;
    Hr actor;
    final String payload = """
        {"employeeId":10,"expectedDepartmentId":1,"expectedPositionId":1,
         "departmentId":2,"positionId":2,"reason":"组织调整","remark":"",
         "requestId":"12345678-1234-1234-1234-123456789012"}
        """;

    @BeforeEach void setup() {
        service = mock(EmployeeTransferService.class);
        access = mock(TransferAccessService.class);
        actor = new Hr(); actor.setId(7);
        when(access.requireMenu("/per/mv")).thenReturn(actor);
        mvc = MockMvcBuilders.standaloneSetup(new EmployeeTransferController(service, access), new TransferLogController(service, access))
                .setControllerAdvice(new GlobalExceptionHandler(), new TransferExceptionHandler()).build();
    }
    @Test void postBindsCommandAndUsesAuthenticatedActor() throws Exception {
        TransferRecord record = new TransferRecord(); record.setId(1);
        when(service.transfer(any(TransferCommand.class), same(actor))).thenReturn(record);
        mvc.perform(post("/personnel/remove/").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200)).andExpect(jsonPath("$.obj.id").value(1));
        verify(service).transfer(argThat(command -> command.employeeId() == 10 && command.expectedDepartmentId() == 1
                && command.departmentId() == 2 && "组织调整".equals(command.reason())), same(actor));
    }
    @Test void deniedRequestsNeverReachServiceIncludingEmployeeLookup() throws Exception {
        when(access.requireMenu(anyString())).thenThrow(new AccessDeniedException("没有权限"));
        mvc.perform(post("/personnel/remove/").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mvc.perform(get("/personnel/remove/employees")).andExpect(status().isForbidden());
        mvc.perform(get("/personnel/remove/options")).andExpect(status().isForbidden());
        mvc.perform(get("/personnel/remove/")).andExpect(status().isForbidden());
        mvc.perform(get("/system/log/")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void businessValidationReturnsBadRequestWithUsefulMessage() throws Exception {
        when(service.transfer(any(), same(actor))).thenThrow(new IllegalArgumentException("员工部门或职位已被修改"));
        mvc.perform(post("/personnel/remove/").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("员工部门或职位已被修改"));
    }
    @Test void logFiltersBindDatesAndLogHasNoMutationEndpoint() throws Exception {
        RespPageBean page = new RespPageBean(); page.setTotal(0L); page.setData(List.of());
        when(service.logs(anyInt(),anyInt(),any(),any(),any(),any())).thenReturn(page);
        mvc.perform(get("/system/log/").param("keyword","员工").param("operator","人事")
                .param("startDate","2026-10-01").param("endDate","2026-10-30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        verify(access).requireMenu("/sys/log");
        verify(service).logs(1,10,"员工","人事",LocalDate.of(2026,10,1),LocalDate.of(2026,10,30));
        mvc.perform(delete("/system/log/")).andExpect(status().isMethodNotAllowed());
    }
    @Test void databaseFailureUsesHttpErrorEvenWhenGlobalAdviceMatchesItsCause() throws Exception {
        when(service.transfer(any(), same(actor))).thenThrow(new DataIntegrityViolationException("test failure",
                new SQLIntegrityConstraintViolationException("private database detail")));
        mvc.perform(post("/personnel/remove/").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败，请刷新记录核实后重试；如持续失败，请联系管理员检查数据库迁移"));
    }
}
