package org.javaboy.vhr.controller.system;

import org.javaboy.vhr.model.RespPageBean;
import org.javaboy.vhr.service.EmployeeTransferService;
import org.javaboy.vhr.service.TransferAccessService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/system/log")
public class TransferLogController {
    private final EmployeeTransferService service;
    private final TransferAccessService access;
    public TransferLogController(EmployeeTransferService service, TransferAccessService access) { this.service = service; this.access = access; }
    @GetMapping("/")
    public RespPageBean list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String operator,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        access.requireMenu("/sys/log");
        return service.logs(page, size, keyword, operator, startDate, endDate);
    }
}
