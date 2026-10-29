package org.javaboy.vhr.service;

import org.javaboy.vhr.mapper.EcMapper;
import org.javaboy.vhr.mapper.TransferMapper;
import org.javaboy.vhr.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;

@Service
public class EmployeeEcService {
    private final EcMapper mapper;
    private final TransferMapper employees;
    public EmployeeEcService(EcMapper mapper, TransferMapper employees) { this.mapper = mapper; this.employees = employees; }

    @Transactional(rollbackFor = Exception.class)
    public Integer saveRule(Integer id, EcRuleCommand command, Hr actor) {
        if (command == null) throw invalid("请填写规则");
        String name = required(command.name(),64,"规则名称");
        String description = text(command.description(),255,"规则说明");
        type(command.type(),false);
        BigDecimal amount = amount(command.amount());
        if (command.enabled() == null) throw invalid("请选择启用状态");
        String key = key(command.requestId());
        String hash = hash("RULE",id,command.version(),name,command.type(),amount,description,command.enabled());
        EcRule before = id == null ? null : mapper.lockRule(id);
        Integer replay = replay(key,hash,actor);
        if (replay != null) return replay;
        if (id != null && before == null) throw invalid("规则不存在");
        if (id != null && !Objects.equals(before.getVersion(),command.version())) throw invalid("规则已被其他人修改，请重新打开后编辑");
        EcRule rule = new EcRule();
        rule.setId(id); rule.setName(name); rule.setType(command.type()); rule.setAmount(amount);
        rule.setDescription(description); rule.setEnabled(command.enabled());
        rule.setVersion(id == null ? 0 : command.version()); rule.setCreatorId(actor.getId());
        rule.setRequestId(key); rule.setCreatedAt(now()); rule.setUpdatedAt(rule.getCreatedAt());
        changed(id == null ? mapper.insertRule(rule) : mapper.updateRule(rule));
        String details = (before == null ? "" : "修改前：\n" + ruleSummary(before) + "\n\n") + "保存后：\n" + ruleSummary(rule);
        audit(key,hash,actor,id == null ? "RULE_CREATE" : "RULE_UPDATE",rule.getId(),
                (id == null ? "新增奖惩规则 #" : "修改奖惩规则 #") + rule.getId() + "：" + name,details);
        return rule.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer register(EcCommand command, Hr actor) {
        if (command == null || command.employeeId() == null || command.ruleId() == null || command.expectedRuleVersion() == null)
            throw invalid("请选择员工和奖惩规则");
        String reason = required(command.reason(),255,"奖惩原因");
        String remark = text(command.remark(),255,"备注");
        BigDecimal amount = amount(command.amount());
        if (command.date() == null || command.date().isBefore(LocalDate.of(1900,1,1)) || command.date().isAfter(now().toLocalDate()))
            throw invalid("发生日期须在 1900 年至今天之间");
        String key = key(command.requestId());
        String hash = hash("REGISTER",command.employeeId(),command.ruleId(),command.expectedRuleVersion(),command.date(),amount,reason,remark);
        TransferEmployee employee = employees.lockEmployee(command.employeeId());
        EcRule rule = mapper.lockRule(command.ruleId());
        Integer replay = replay(key,hash,actor);
        if (replay != null) return replay;
        if (employee == null || !"在职".equals(employee.getWorkState())) throw invalid("只能为在职员工登记奖惩");
        if (rule == null || !Boolean.TRUE.equals(rule.getEnabled())) throw invalid("奖惩规则不存在或已停用，请重新选择");
        if (!Objects.equals(rule.getVersion(),command.expectedRuleVersion())) throw invalid("规则已被修改，请重新选择规则并核对金额");
        Department department = employee.getDepartmentId() == null ? null : employees.department(employee.getDepartmentId());
        EcRecord record = new EcRecord();
        record.setEid(employee.getId()); record.setEmployeeName(employee.getName()); record.setWorkId(employee.getWorkId());
        record.setDepartmentName(department == null ? null : department.getName());
        record.setEcDate(command.date()); record.setEcType(rule.getType()); record.setEcReason(reason); record.setRemark(remark);
        record.setRuleId(rule.getId()); record.setRuleName(rule.getName()); record.setRuleVersion(rule.getVersion());
        record.setRuleDescription(rule.getDescription()); record.setDefaultAmount(rule.getAmount()); record.setAmount(amount);
        record.setStatus("ACTIVE"); record.setOperatorId(actor.getId()); record.setOperatorName(actorName(actor)); record.setCreatedAt(now());
        changed(mapper.insertRecord(record));
        audit(key,hash,actor,"EC_REGISTER",record.getId(),"登记员工奖惩 #" + record.getId() + "：" + record.getEmployeeName() + "（" + record.getWorkId() + "）",recordSummary(record));
        return record.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer cancel(Integer id, EcCancelCommand command, Hr actor) {
        if (command == null) throw invalid("请填写撤销原因");
        String reason = required(command.reason(),255,"撤销原因");
        String key = key(command.requestId());
        String hash = hash("CANCEL",id,reason);
        EcRecord record = mapper.lockRecord(id);
        Integer replay = replay(key,hash,actor);
        if (replay != null) return replay;
        if (record == null) throw invalid("奖惩记录不存在");
        if (!"ACTIVE".equals(record.getStatus())) throw invalid("该记录已撤销，请刷新列表");
        record.setCancelledBy(actor.getId()); record.setCancelledByName(actorName(actor));
        record.setCancelledAt(now()); record.setCancelReason(reason);
        changed(mapper.cancelRecord(record));
        audit(key,hash,actor,"EC_CANCEL",id,"撤销员工奖惩 #" + id + "：" + Objects.toString(record.getEmployeeName(),"历史员工")
                + "（" + Objects.toString(record.getWorkId(),"未记录") + "）",recordSummary(record) + "\n撤销原因：" + reason);
        return id;
    }

    @Transactional(readOnly = true)
    public RespPageBean rules(int page,int size,String keyword,Integer type,Boolean enabled) {
        EcQuery query = query(page,size,keyword,type,null,enabled,null,null);
        return page(mapper.ruleCount(query),mapper.rules(query));
    }
    @Transactional(readOnly = true)
    public RespPageBean records(int page,int size,String keyword,Integer type,String status,LocalDate start,LocalDate end) {
        EcQuery query = query(page,size,keyword,type,status,null,start,end);
        return page(mapper.recordCount(query),mapper.records(query));
    }
    public List<TransferEmployee> employees(String keyword) { return employees.searchEmployees(text(keyword,64,"搜索词")); }

    private EcQuery query(int page,int size,String keyword,Integer type,String status,Boolean enabled,LocalDate start,LocalDate end) {
        if (page < 1 || page > 100000 || size < 1 || size > 100) throw invalid("分页参数无效");
        type(type,true);
        if (status != null && !status.isEmpty() && !Set.of("ACTIVE","CANCELLED").contains(status)) throw invalid("记录状态无效");
        if (start != null && end != null && start.isAfter(end)) throw invalid("开始日期不能晚于结束日期");
        return new EcQuery((page-1)*size,size,text(keyword,64,"搜索词"),type,status,enabled,start,end);
    }
    private RespPageBean page(long total,List<?> data) { RespPageBean page = new RespPageBean(); page.setTotal(total); page.setData(data); return page; }
    private Integer replay(String key,String hash,Hr actor) {
        if (actor == null || actor.getId() == null) throw invalid("操作员身份无效");
        EcAudit audit = mapper.receipt(key);
        if (audit == null) return null;
        if (!Objects.equals(audit.getHrid(),actor.getId()) || !Objects.equals(audit.getRequestHash(),hash))
            throw invalid("请求标识已用于其他操作，请刷新记录后重新操作");
        return audit.getEcResultId();
    }
    private void audit(String key,String hash,Hr actor,String action,Integer id,String summary,String details) {
        EcAudit audit = new EcAudit();
        audit.setRequestKey(key); audit.setRequestHash(hash); audit.setHrid(actor.getId()); audit.setOperatorName(actorName(actor));
        audit.setEcAction(action); audit.setEcResultId(id); audit.setOperate(summary); audit.setDetails(details); audit.setCreatedAt(now());
        changed(mapper.insertAudit(audit));
    }
    private String ruleSummary(EcRule rule) {
        return "名称：" + rule.getName() + "\n类型：" + typeName(rule.getType()) + "\n默认金额：" + rule.getAmount()
                + "\n状态：" + (Boolean.TRUE.equals(rule.getEnabled()) ? "启用" : "停用") + "\n说明：" + rule.getDescription();
    }
    private String recordSummary(EcRecord record) {
        return "规则：" + Objects.toString(record.getRuleName(),"历史未记录") + "\n类型：" + typeName(record.getEcType())
                + "\n发生日期：" + record.getEcDate() + "\n金额：" + Objects.toString(record.getAmount(),"历史未记录")
                + "\n原因：" + record.getEcReason() + "\n备注：" + Objects.toString(record.getRemark(),"");
    }
    private String typeName(Integer type) { return Objects.equals(type,0) ? "奖励" : Objects.equals(type,1) ? "处罚" : "历史未记录"; }
    private void type(Integer type,boolean optional) { if (!(optional && type == null) && !Objects.equals(type,0) && !Objects.equals(type,1)) throw invalid("奖惩类型须为奖励或处罚"); }
    private BigDecimal amount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(new BigDecimal("99999999.99")) > 0 || amount.stripTrailingZeros().scale() > 2)
            throw invalid("金额须为 0 至 99999999.99，最多两位小数");
        return amount.setScale(2);
    }
    private String text(String value,int max,String label) { String result = value == null ? "" : value.trim(); if (result.length() > max) throw invalid(label + "不能超过" + max + "个字符"); return result; }
    private String required(String value,int max,String label) { String result = text(value,max,label); if (result.isBlank()) throw invalid("请填写" + label); return result; }
    private String key(String value) {
        try { String key = UUID.fromString(value).toString(); if (!key.equalsIgnoreCase(value)) throw invalid(""); return key; }
        catch (RuntimeException e) { throw invalid("请求标识无效，请重新打开窗口"); }
    }
    // Length-prefixed fields distinguish null and delimiter-containing input.
    private String hash(Object... fields) {
        StringBuilder value = new StringBuilder();
        for (Object field : fields) { String text = field == null ? null : field.toString(); value.append(text == null ? "-1:" : text.length() + ":" + text); }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private void changed(int rows) { if (rows != 1) throw new IllegalStateException("写入未完成，事务已回滚"); }
    private String actorName(Hr actor) { return actor.getName() == null ? actor.getUsername() : actor.getName(); }
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Shanghai")); }
    private IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
