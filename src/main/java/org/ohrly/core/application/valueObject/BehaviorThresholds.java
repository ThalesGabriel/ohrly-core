package org.ohrly.core.application.valueObject;

public record BehaviorThresholds(
        int sustainedDegradationDays,
        int preIncidentDays,
        double attentionMultiplier,
        double degradationMultiplier,
        double preIncidentMultiplier
) {

    public static BehaviorThresholds defaultThresholds() {
        return new BehaviorThresholds(
                3,
                2,
                1.2,
                1.5,
                2.0
        );
    }
}
