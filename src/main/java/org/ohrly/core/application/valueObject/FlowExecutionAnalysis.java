package org.ohrly.core.application.valueObject;

import org.ohrly.core.application.domain.FlowConsistencyScore;
import org.ohrly.core.application.type.FlowOutcomeType;

import java.util.List;

public record FlowExecutionAnalysis(
        String flowId,
        String sessionId,
        int consistencyScore,
        FlowConsistencyScore consistencyState,
        FlowOutcomeType outcome,
        List<BehavioralConstruct> constructs
) {}
