package org.javaboy.vhr.model;
import java.math.BigDecimal;
public record EcRuleCommand(Integer version, String name, Integer type, BigDecimal amount,
                            String description, Boolean enabled, String requestId) {}

