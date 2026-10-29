package org.javaboy.vhr.model;

public class EcRule {
    private Integer id;
    private Integer type;
    private Integer version;
    private Integer creatorId;
    private String name;
    private String description;
    private String requestId;
    private java.math.BigDecimal amount;
    private Boolean enabled;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Integer getCreatorId() { return creatorId; }
    public void setCreatorId(Integer creatorId) { this.creatorId = creatorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

