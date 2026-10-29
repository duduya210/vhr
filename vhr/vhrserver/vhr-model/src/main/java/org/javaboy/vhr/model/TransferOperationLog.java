package org.javaboy.vhr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransferOperationLog {
    private Integer id;
    private Integer transferId;
    private Integer operatorId;
    private String operatorName;
    private LocalDateTime createdAt;
    private String operate;
    private String employeeName;
    private String workId;
    private String beforeDepName;
    private String beforeJobName;
    private String afterDepName;
    private String afterJobName;
    private String reason;
    private String remark;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getTransferId() { return transferId; }
    public void setTransferId(Integer transferId) { this.transferId = transferId; }
    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getOperate() { return operate; }
    public void setOperate(String operate) { this.operate = operate; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getWorkId() { return workId; }
    public void setWorkId(String workId) { this.workId = workId; }
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
}

