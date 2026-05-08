package org.ohrly.core.valueObjects;

public record BehaviorImpact(
        int durationDays,
        int impactedEvents,
        double excessValue,
        double impactedValue,
        String metricName
) {
    public static BehaviorImpact empty(String metricName) {
        return new BehaviorImpact(
                0,
                0,
                0,
                0,
                metricName
        );
    }
}
