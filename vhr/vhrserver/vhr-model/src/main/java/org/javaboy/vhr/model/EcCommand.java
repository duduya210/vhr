package org.javaboy.vhr.model;
import java.math.BigDecimal;
import java.time.LocalDate;
public record EcCommand(Integer employeeId, Integer ruleId, Integer expectedRuleVersion,
                        LocalDate date, BigDecimal amount, String reason, String remark, String requestId) {}

