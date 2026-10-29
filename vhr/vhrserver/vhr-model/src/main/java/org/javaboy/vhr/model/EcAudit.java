package org.javaboy.vhr.model;

public class EcAudit {
    private Integer id;
    private Integer hrid;
    private Integer ecResultId;
    private String operate;
    private String operatorName;
    private String details;
    private String requestKey;
    private String requestHash;
    private String ecAction;
    private java.time.LocalDateTime createdAt;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getHrid() { return hrid; }
    public void setHrid(Integer hrid) { this.hrid = hrid; }
    public Integer getEcResultId() { return ecResultId; }
    public void setEcResultId(Integer ecResultId) { this.ecResultId = ecResultId; }
    public String getOperate() { return operate; }
    public void setOperate(String operate) { this.operate = operate; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public String getEcAction() { return ecAction; }
    public void setEcAction(String ecAction) { this.ecAction = ecAction; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}

