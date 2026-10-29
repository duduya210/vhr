package org.javaboy.vhr.controller.system.basic;
import org.javaboy.vhr.model.*;
import org.javaboy.vhr.service.EmployeeEcService;
import org.javaboy.vhr.service.TransferAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/basic/ec")
public class EcRuleController {
    private final EmployeeEcService service;
    private final TransferAccessService access;
    public EcRuleController(EmployeeEcService service,TransferAccessService access) { this.service=service; this.access=access; }
    @GetMapping("/")
    public RespPageBean list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String keyword,@RequestParam(required=false) Integer type,@RequestParam(required=false) Boolean enabled) {
        access.requireMenu("/sys/basic"); return service.rules(page,size,keyword,type,enabled);
    }
    @PostMapping("/")
    public RespBean create(@RequestBody EcRuleCommand command) {
        return RespBean.ok("奖惩规则已创建",service.saveRule(null,command,access.requireMenu("/sys/basic")));
    }
    @PutMapping("/{id}")
    public RespBean update(@PathVariable Integer id,@RequestBody EcRuleCommand command) {
        return RespBean.ok("奖惩规则已保存",service.saveRule(id,command,access.requireMenu("/sys/basic")));
    }
}
