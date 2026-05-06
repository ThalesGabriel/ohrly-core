package org.ohrly.core.services;

import org.ohrly.core.domain.FlowConsistencyScore;
import org.ohrly.core.domain.FlowEvaluation;
import org.ohrly.core.domain.FlowFinding;
import org.ohrly.core.enums.ConsistencyLevel;
import org.ohrly.core.enums.FindingSeverity;

import java.util.Objects;

public class ConsistencyScorerService {

    private static final int MAX_SCORE = 100;
    private static final int MIN_SCORE = 0;

    public FlowConsistencyScore score(FlowEvaluation evaluation) {
        Objects.requireNonNull(evaluation, "evaluation cannot be null");

        int penalty = evaluation.getFindings().stream()
                .mapToInt(this::penaltyFor)
                .sum();

        int finalScore = Math.max(MIN_SCORE, MAX_SCORE - penalty);

        return new FlowConsistencyScore(
                evaluation.getFlowId(),
                evaluation.getSessionId(),
                finalScore,
                consistencyLevel(finalScore)
        );
    }

    private int penaltyFor(FlowFinding finding) {
        return switch (finding.type()) {
            case MISSING_FINAL_STEP -> 40;
            case TIMEOUT -> 35;
            case HANDOFF -> 30;
            case MISSING_REQUIRED_STEP -> penaltyBySeverity(finding.severity());
            case LATE_EVENTS -> 10;
        };
    }

    private int penaltyBySeverity(FindingSeverity severity) {
        return switch (severity) {
            case HIGH -> 30;
            case MEDIUM -> 15;
            case LOW -> 5;
        };
    }

    private ConsistencyLevel consistencyLevel(int score) {
        if (score >= 85) {
            return ConsistencyLevel.HEALTHY;
        }

        if (score >= 70) {
            return ConsistencyLevel.ATTENTION;
        }

        return ConsistencyLevel.DEGRADED;
    }
}
