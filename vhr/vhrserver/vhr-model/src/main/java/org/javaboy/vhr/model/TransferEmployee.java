package org.javaboy.vhr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransferEmployee {
    private Integer id;
    private String name;
    private String workId;
    private Integer departmentId;
    private Integer positionId;
    private String departmentName;
    private String positionName;
    private String workState;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWorkId() { return workId; }
    public void setWorkId(String workId) { this.workId = workId; }
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    public Integer getPositionId() { return positionId; }
    public void setPositionId(Integer positionId) { this.positionId = positionId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public String getWorkState() { return workState; }
    public void setWorkState(String workState) { this.workState = workState; }
}

