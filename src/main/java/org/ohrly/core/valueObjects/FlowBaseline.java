package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.MetricType;

public record FlowBaseline(
        FlowContext context,
        String metricName,
        double expectedValue,
        double p95
) {}
