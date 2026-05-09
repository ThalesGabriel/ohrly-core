package org.ohrly.core.application.valueObject;

import org.ohrly.core.domain.valueObject.FlowContext;

public record BehaviorPrecedence(
        FlowContext context,
        String metricName,
        boolean matchesHistoricalPattern,
        double similarityScore,
        String message
) {
    public static BehaviorPrecedence empty(FlowContext context, String metricName, String message) {
        return new BehaviorPrecedence(context, metricName, false, 0, message);
    }
}
