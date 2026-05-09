package org.ohrly.core.application.valueObject;

import org.ohrly.core.domain.valueObject.FlowContext;

public record FlowBaseline(
        FlowContext context,
        String metricName,
        double expectedValue,
        double p95
) {}
