package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehaviorState;

public record BehaviorAnalysisResult(
        Context context,
        BehaviorState state,
        double currentAverage,
        double expectedAverage,
        int durationDays,
        String message
) { }
