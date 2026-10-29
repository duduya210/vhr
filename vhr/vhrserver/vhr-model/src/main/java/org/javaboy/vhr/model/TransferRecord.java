package org.javaboy.vhr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransferRecord {
    private Integer id;
    private Integer eid;
    private String employeeName;
    private String workId;
    private Integer beforeDepId;
    private Integer beforeJobId;
    private Integer afterDepId;
    private Integer afterJobId;
    private String beforeDepName;
    private String beforeJobName;
    private String afterDepName;
    private String afterJobName;
    private String reason;
    private String remark;
    private LocalDate removeDate;
    private LocalDateTime createdAt;
    private Integer operatorId;
    private String operatorName;
    private String requestId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEid() { return eid; }
    public void setEid(Integer eid) { this.eid = eid; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getWorkId() { return workId; }
    public void setWorkId(String workId) { this.workId = workId; }
    public Integer getBeforeDepId() { return beforeDepId; }
    public void setBeforeDepId(Integer beforeDepId) { this.beforeDepId = beforeDepId; }
    public Integer getBeforeJobId() { return beforeJobId; }
    public void setBeforeJobId(Integer beforeJobId) { this.beforeJobId = beforeJobId; }
    public Integer getAfterDepId() { return afterDepId; }
    public void setAfterDepId(Integer afterDepId) { this.afterDepId = afterDepId; }
    public Integer getAfterJobId() { return afterJobId; }
    public void setAfterJobId(Integer afterJobId) { this.afterJobId = afterJobId; }
    public String getBeforeDepName() { return beforeDepName; }
    public void setBeforeDepName(String beforeDepName) { this.beforeDepName = beforeDepName; }
    public String getBeforeJobName() { return beforeJobName; }
    public void setBeforeJobName(String beforeJobName) { this.beforeJobName = beforeJobName; }
    public String getAfterDepName() { return afterDepName; }
    public void setAfterDepName(String afterDepName) { this.afterDepName = afterDepName; }
    public String getAfterJobName() { return afterJobName; }
    public void setAfterJobName(String afterJobName) { this.afterJobName = afterJobName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDate getRemoveDate() { return removeDate; }
    public void setRemoveDate(LocalDate removeDate) { this.removeDate = removeDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}

