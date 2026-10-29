package org.javaboy.vhr.model;

import java.time.LocalDateTime;

public record TransferQuery(int offset, int size, String keyword, Integer departmentId,
                            String operator, LocalDateTime startTime, LocalDateTime endTime) {}
