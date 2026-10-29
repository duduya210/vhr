package org.javaboy.vhr.model;

/** Operator and execution time are supplied by the server, never by this command. */
public record TransferCommand(Integer employeeId, Integer expectedDepartmentId, Integer expectedPositionId,
                              Integer departmentId, Integer positionId, String reason, String remark,
                              String requestId) {}
