package org.ohrly.core.valueObjects;

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
