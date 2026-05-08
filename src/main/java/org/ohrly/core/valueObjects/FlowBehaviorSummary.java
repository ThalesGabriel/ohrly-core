package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehavioralConstructType;

import java.util.Map;

public record FlowBehaviorSummary(
        int totalSessions,
        double cleanCompletionRate,
        double frictionRate,
        double ruptureRate,
        double escalationRate,
        double recoveryRate,
        double continuityLossRate,
        double abandonRate,
        double timeoutRate,
        double averageFrictionScore,
        Map<BehavioralConstructType, Long> constructCounts
) {}
