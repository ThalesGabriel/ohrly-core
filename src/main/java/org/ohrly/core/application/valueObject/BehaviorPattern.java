package org.ohrly.core.application.valueObject;

import org.ohrly.core.domain.valueObject.FlowContext;

import java.time.LocalDate;
import java.util.List;

public record BehaviorPattern(
        FlowContext context,
        String metricName,
        LocalDate criticalDate,
        List<Double> ratiosBeforeCritical
) { }