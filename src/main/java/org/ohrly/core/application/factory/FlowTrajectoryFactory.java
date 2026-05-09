package org.ohrly.core.application.factory;

import org.ohrly.core.application.service.FlowTrajectoryAwareBehavioralConstructExtractorService;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.application.type.FlowOutcomeType;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class FlowTrajectoryFactory {

    @Autowired
    private FlowTrajectoryAwareBehavioralConstructExtractorService constructExtractor;

    public FlowTrajectory create(
            String flowId,
            String sessionId,
            List<BehavioralPrimitive> primitives
    ) {
        if (primitives == null || primitives.isEmpty()) {
            throw new IllegalArgumentException("primitives must not be empty");
        }

        List<BehavioralPrimitive> orderedPrimitives = primitives.stream()
                .sorted(Comparator.comparing(BehavioralPrimitive::timestamp))
                .toList();

        return new FlowTrajectory(
                sessionId,
                flowId,
                inferStartedAt(orderedPrimitives),
                inferFinishedAt(orderedPrimitives),
                inferOutcome(orderedPrimitives),
                orderedPrimitives,
                constructExtractor.extract(orderedPrimitives),
                inferContext(orderedPrimitives)
        );
    }

    private Instant inferStartedAt(List<BehavioralPrimitive> primitives) {
        return primitives.getFirst().timestamp();
    }

    private Instant inferFinishedAt(List<BehavioralPrimitive> primitives) {
        return primitives.stream()
                .filter(primitive -> isTerminal(primitive.type()))
                .map(BehavioralPrimitive::timestamp)
                .findFirst()
                .orElse(null);
    }

    private FlowOutcomeType inferOutcome(List<BehavioralPrimitive> primitives) {
        List<BehavioralPrimitiveType> types = primitives.stream()
                .map(BehavioralPrimitive::type)
                .toList();

        if (types.contains(BehavioralPrimitiveType.COMPLETE)) {
            return FlowOutcomeType.COMPLETED;
        }

        if (types.contains(BehavioralPrimitiveType.ABANDON)) {
            return FlowOutcomeType.ABANDONED;
        }

        if (types.contains(BehavioralPrimitiveType.TIMEOUT)) {
            return FlowOutcomeType.TIMED_OUT;
        }

        if (types.contains(BehavioralPrimitiveType.HUMAN_HANDOFF)
                || types.contains(BehavioralPrimitiveType.TRANSFER)) {
            return FlowOutcomeType.TRANSFERRED;
        }

        return FlowOutcomeType.OPEN;
    }

    private boolean isTerminal(BehavioralPrimitiveType type) {
        return type == BehavioralPrimitiveType.COMPLETE
                || type == BehavioralPrimitiveType.ABANDON
                || type == BehavioralPrimitiveType.TIMEOUT
                || type == BehavioralPrimitiveType.HUMAN_HANDOFF
                || type == BehavioralPrimitiveType.TRANSFER;
    }

    private Map<String, Object> inferContext(List<BehavioralPrimitive> primitives) {
        return Map.of();
    }
}
