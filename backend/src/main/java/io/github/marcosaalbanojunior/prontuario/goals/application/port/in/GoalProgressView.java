package io.github.marcosaalbanojunior.prontuario.goals.application.port.in;

import java.math.BigDecimal;

/** Progress of one active goal; type and period are stored codes ("water", "week"). */
public record GoalProgressView(String type, String period, BigDecimal target, BigDecimal achieved, String unit,
                               int percent, boolean met) {}
