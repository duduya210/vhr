package org.javaboy.vhr.service;

import org.javaboy.vhr.mapper.TransferMapper;
import org.javaboy.vhr.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class EmployeeTransferService {
    private final TransferMapper mapper;
    public EmployeeTransferService(TransferMapper mapper) { this.mapper = mapper; }

    @Transactional(rollbackFor = Exception.class)
    public TransferRecord transfer(TransferCommand command, Hr actor) {
        if (command == null || command.employeeId() == null || command.departmentId() == null || command.positionId() == null)
            throw new IllegalArgumentException("请选择员工、目标部门和目标职位");
        if (actor == null || actor.getId() == null) throw new IllegalArgumentException("操作员身份无效");
        String reason = text(command.reason(), 255, "调动原因");
        String remark = text(command.remark(), 255, "备注");
        if (reason.isBlank()) throw new IllegalArgumentException("请填写调动原因");
        String requestId;
        try {
            requestId = UUID.fromString(command.requestId()).toString();
            if (!requestId.equalsIgnoreCase(command.requestId())) throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw new IllegalArgumentException("请求标识无效，请重新打开调动窗口"); }

        // Serialize transfers of one employee, including retries with the same request ID.
        TransferEmployee employee = mapper.lockEmployee(command.employeeId());
        if (employee == null) throw new IllegalArgumentException("员工不存在，请刷新后重试");
        TransferRecord existing = mapper.findByRequestId(requestId);
        if (existing != null) {
            if (!Objects.equals(existing.getEid(), command.employeeId())
                    || !Objects.equals(existing.getOperatorId(), actor.getId())
                    || !Objects.equals(existing.getBeforeDepId(), command.expectedDepartmentId())
                    || !Objects.equals(existing.getBeforeJobId(), command.expectedPositionId())
                    || !Objects.equals(existing.getAfterDepId(), command.departmentId())
                    || !Objects.equals(existing.getAfterJobId(), command.positionId())
                    || !Objects.equals(existing.getReason(), reason) || !Objects.equals(existing.getRemark(), remark))
                throw new IllegalArgumentException("请求标识已用于其他调动，请刷新记录后重新操作");
            return existing;
        }
        if (!"在职".equals(employee.getWorkState())) throw new IllegalArgumentException("仅支持调动在职员工");
        if (!Objects.equals(employee.getDepartmentId(), command.expectedDepartmentId())
                || !Objects.equals(employee.getPositionId(), command.expectedPositionId()))
            throw new IllegalArgumentException("员工部门或职位已被修改，请重新选择员工后提交");
        if (Objects.equals(employee.getDepartmentId(), command.departmentId())
                && Objects.equals(employee.getPositionId(), command.positionId()))
            throw new IllegalArgumentException("目标部门和职位与当前相同，无需调动");
        Department targetDepartment = mapper.department(command.departmentId());
        Position targetPosition = mapper.position(command.positionId());
        if (targetDepartment == null || !Boolean.TRUE.equals(targetDepartment.getEnabled()))
            throw new IllegalArgumentException("目标部门不存在或已停用");
        if (targetPosition == null || !Boolean.TRUE.equals(targetPosition.getEnabled()))
            throw new IllegalArgumentException("目标职位不存在或已停用");
        Department sourceDepartment = employee.getDepartmentId() == null ? null : mapper.department(employee.getDepartmentId());
        Position sourcePosition = employee.getPositionId() == null ? null : mapper.position(employee.getPositionId());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        TransferRecord record = new TransferRecord();
        record.setEid(employee.getId());
        record.setEmployeeName(employee.getName());
        record.setWorkId(employee.getWorkId());
        record.setBeforeDepId(employee.getDepartmentId());
        record.setBeforeJobId(employee.getPositionId());
        record.setBeforeDepName(sourceDepartment == null ? null : sourceDepartment.getName());
        record.setBeforeJobName(sourcePosition == null ? null : sourcePosition.getName());
        record.setAfterDepId(targetDepartment.getId());
        record.setAfterJobId(targetPosition.getId());
        record.setAfterDepName(targetDepartment.getName());
        record.setAfterJobName(targetPosition.getName());
        record.setReason(reason);
        record.setRemark(remark);
        record.setOperatorId(actor.getId());
        record.setOperatorName(actor.getName() == null ? actor.getUsername() : actor.getName());
        record.setCreatedAt(now);
        record.setRemoveDate(now.toLocalDate());
        record.setRequestId(requestId);
        if (mapper.updateAssignment(record) != 1 || mapper.insertTransfer(record) != 1 || mapper.insertLog(record) != 1)
            throw new IllegalStateException("调动写入不完整，事务已回滚");
        return record;
    }

    @Transactional(readOnly = true)
    public RespPageBean transfers(int page, int size, String keyword, Integer departmentId, LocalDate start, LocalDate end) {
        TransferQuery q = query(page, size, keyword, departmentId, null, start, end);
        RespPageBean result = new RespPageBean();
        result.setTotal(mapper.transferCount(q));
        result.setData(mapper.transfers(q));
        return result;
    }

    @Transactional(readOnly = true)
    public RespPageBean logs(int page, int size, String keyword, String operator, LocalDate start, LocalDate end) {
        TransferQuery q = query(page, size, keyword, null, operator, start, end);
        RespPageBean result = new RespPageBean();
        result.setTotal(mapper.logCount(q));
        result.setData(mapper.logs(q));
        return result;
    }

    public List<TransferEmployee> employees(String keyword) { return mapper.searchEmployees(text(keyword, 64, "搜索词")); }
    public Map<String, Object> options() {
        return Map.of("departments", mapper.departments(), "positions", mapper.positions());
    }

    private TransferQuery query(int page, int size, String keyword, Integer departmentId, String operator, LocalDate start, LocalDate end) {
        if (page < 1 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException("分页参数无效");
        if (start != null && end != null && start.isAfter(end)) throw new IllegalArgumentException("开始日期不能晚于结束日期");
        return new TransferQuery((page - 1) * size, size, text(keyword, 64, "搜索词"), departmentId,
                text(operator, 64, "操作员"), start == null ? null : start.atStartOfDay(), end == null ? null : end.plusDays(1).atStartOfDay());
    }
    private String text(String value, int max, String label) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max) throw new IllegalArgumentException(label + "不能超过" + max + "个字符");
        return result;
    }
}
