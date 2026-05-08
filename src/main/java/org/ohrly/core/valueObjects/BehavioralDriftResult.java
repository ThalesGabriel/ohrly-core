package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehavioralDriftStateType;

import java.util.Map;

public record BehavioralDriftResult(
        String flowId,
        BehavioralDriftStateType state,
        double frictionDelta,
        double ruptureDelta,
        double escalationDelta,
        double continuityLossDelta,
        double abandonDelta,
        double cleanCompletionDelta,
        double averageFrictionScoreDelta,
        Map<String, Double> signals
) {

    public boolean isDegraded() {
        return state == BehavioralDriftStateType.DEGRADED
                || state == BehavioralDriftStateType.CRITICAL;
    }
}
