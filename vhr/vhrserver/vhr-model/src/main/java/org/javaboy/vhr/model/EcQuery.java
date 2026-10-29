package org.javaboy.vhr.model;
import java.time.LocalDate;
public record EcQuery(int offset, int size, String keyword, Integer type, String status, Boolean enabled,
                      LocalDate startDate, LocalDate endDate) {}

