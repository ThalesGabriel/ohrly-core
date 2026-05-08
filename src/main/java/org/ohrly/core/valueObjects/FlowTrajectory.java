package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.BehavioralPrimitiveType;
import org.ohrly.core.enums.TrajectoryOutcomeType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FlowTrajectory(
        String sessionId,
        String flowId,
        Instant startedAt,
        Instant finishedAt,
        TrajectoryOutcomeType outcome,
        List<BehavioralPrimitive> primitives,
        List<BehavioralConstruct> constructs,
        Map<String, String> context
) {

    public FlowTrajectory {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }

        if (flowId == null || flowId.isBlank()) {
            throw new IllegalArgumentException("flowId must not be blank");
        }

        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }

        if (outcome == null) {
            throw new IllegalArgumentException("outcome must not be null");
        }

        primitives = primitives == null ? List.of() : List.copyOf(primitives);
        constructs = constructs == null ? List.of() : List.copyOf(constructs);
        context = context == null ? Map.of() : Map.copyOf(context);
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public Duration duration() {
        if (finishedAt == null) {
            return Duration.between(startedAt, Instant.now());
        }

        return Duration.between(startedAt, finishedAt);
    }

    public boolean hasConstruct(BehavioralConstructType type) {
        return constructs.stream()
                .anyMatch(construct -> construct.type() == type);
    }

    public boolean hasPrimitive(BehavioralPrimitiveType type) {
        return primitives.stream()
                .anyMatch(primitive -> primitive.type() == type);
    }

    public boolean completedCleanly() {
        return hasConstruct(BehavioralConstructType.CLEAN_COMPLETION);
    }

    public boolean hasFriction() {
        return hasConstruct(BehavioralConstructType.FRICTION);
    }

    public boolean hasRupture() {
        return hasConstruct(BehavioralConstructType.RUPTURE);
    }

    public boolean hasEscalation() {
        return hasConstruct(BehavioralConstructType.ESCALATION);
    }

    public boolean hasRecovery() {
        return hasConstruct(BehavioralConstructType.RECOVERY);
    }

    public boolean hasContinuityLoss() {
        return hasConstruct(BehavioralConstructType.CONTINUITY_LOSS);
    }
}
