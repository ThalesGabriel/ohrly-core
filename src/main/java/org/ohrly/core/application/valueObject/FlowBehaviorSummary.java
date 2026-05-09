package org.ohrly.core.application.valueObject;

import org.ohrly.core.application.type.BehavioralConstructType;

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
