package org.javaboy.vhr.controller.personnel;

import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.EmployeeTransferService;
import org.javaboy.vhr.service.TransferAccessService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/personnel/remove")
public class EmployeeTransferController {
    private final EmployeeTransferService service;
    private final TransferAccessService access;
    public EmployeeTransferController(EmployeeTransferService service, TransferAccessService access) { this.service = service; this.access = access; }

    @GetMapping("/")
    public RespPageBean list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        access.requireMenu("/per/mv");
        return service.transfers(page, size, keyword, departmentId, startDate, endDate);
    }
    @GetMapping("/employees")
    public List<TransferEmployee> employees(@RequestParam(defaultValue = "") String keyword) {
        access.requireMenu("/per/mv"); return service.employees(keyword);
    }
    @GetMapping("/options")
    public Map<String, Object> options() { access.requireMenu("/per/mv"); return service.options(); }
    @PostMapping("/")
    public RespBean transfer(@RequestBody TransferCommand command) {
        Hr actor = access.requireMenu("/per/mv");
        TransferRecord result = service.transfer(command, actor);
        return RespBean.ok("调动已生效，员工档案与操作日志已同步", result);
    }
}
