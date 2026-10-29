package org.javaboy.vhr.controller.personnel;
import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.EmployeeEcService;
import org.javaboy.vhr.service.TransferAccessService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/personnel/ec")
public class EmployeeEcController {
    private final EmployeeEcService service;
    private final TransferAccessService access;
    public EmployeeEcController(EmployeeEcService service,TransferAccessService access) { this.service=service; this.access=access; }
    @GetMapping("/")
    public RespPageBean list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String keyword,@RequestParam(required=false) Integer type,@RequestParam(required=false) String status,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate) {
        access.requireMenu("/per/ec"); return service.records(page,size,keyword,type,status,startDate,endDate);
    }
    @GetMapping("/employees")
    public List<TransferEmployee> employees(@RequestParam(defaultValue="") String keyword) {
        access.requireMenu("/per/ec"); return service.employees(keyword);
    }
    @GetMapping("/rules")
    public RespPageBean rules(@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="1") int page) {
        access.requireMenu("/per/ec"); return service.rules(page,30,keyword,null,true);
    }
    @PostMapping("/")
    public RespBean register(@RequestBody EcCommand command) {
        return RespBean.ok("奖惩已登记，未计入工资",service.register(command,access.requireMenu("/per/ec")));
    }
    @PostMapping("/{id}/cancel")
    public RespBean cancel(@PathVariable Integer id,@RequestBody EcCancelCommand command) {
        return RespBean.ok("奖惩已撤销，历史记录已保留",service.cancel(id,command,access.requireMenu("/per/ec")));
    }
}
