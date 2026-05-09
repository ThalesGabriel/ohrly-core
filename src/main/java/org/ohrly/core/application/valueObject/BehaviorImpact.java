package org.ohrly.core.application.valueObject;

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
