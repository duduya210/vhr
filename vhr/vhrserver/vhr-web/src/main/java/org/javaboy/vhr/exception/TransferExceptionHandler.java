package org.javaboy.vhr.exception;

import org.javaboy.vhr.controller.personnel.EmployeeTransferController;
import org.javaboy.vhr.controller.personnel.EmployeeEcController;
import org.javaboy.vhr.controller.system.basic.EcRuleController;
import org.javaboy.vhr.controller.system.TransferLogController;
import org.javaboy.vhr.model.RespBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = {EmployeeTransferController.class, TransferLogController.class, EmployeeEcController.class, EcRuleController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransferExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(TransferExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespBean invalid(IllegalArgumentException e) { return RespBean.error(e.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RespBean denied(AccessDeniedException e) { return RespBean.error(e.getMessage()); }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RespBean failed(RuntimeException e) {
        logger.error("人事操作或日志访问失败", e);
        return RespBean.error("操作失败，请刷新记录核实后重试；如持续失败，请联系管理员检查数据库迁移");
    }
}
