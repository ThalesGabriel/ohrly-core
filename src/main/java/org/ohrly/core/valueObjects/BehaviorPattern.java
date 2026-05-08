package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.MetricType;

import java.time.LocalDate;
import java.util.List;

public record BehaviorPattern(
        FlowContext context,
        String metricName,
        LocalDate criticalDate,
        List<Double> ratiosBeforeCritical
) { }