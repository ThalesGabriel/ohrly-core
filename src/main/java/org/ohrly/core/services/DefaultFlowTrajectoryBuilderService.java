package org.ohrly.core.services;

import org.ohrly.core.BehavioralConstructExtractor;
import org.ohrly.core.FlowTrajectoryBuilder;
import org.ohrly.core.enums.BehavioralPrimitiveType;
import org.ohrly.core.enums.TrajectoryOutcomeType;
import org.ohrly.core.valueObjects.BehavioralPrimitive;
import org.ohrly.core.valueObjects.FlowTrajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DefaultFlowTrajectoryBuilderService implements FlowTrajectoryBuilder {

    @Autowired
    @Qualifier("trajectoryAwareBehavioralConstructExtractorService")
    private BehavioralConstructExtractor constructExtractor;

    @Override
    public FlowTrajectory build(
            String flowId,
            String sessionId,
            List<BehavioralPrimitive> primitives
    ) {
        if (primitives == null || primitives.isEmpty()) {
            throw new IllegalArgumentException("primitives must not be empty");
        }

        var orderedPrimitives = primitives.stream()
                .sorted(Comparator.comparing(BehavioralPrimitive::timestamp))
                .toList();

        var startedAt = orderedPrimitives.getFirst().timestamp();
        var finishedAt = inferFinishedAt(orderedPrimitives);
        var outcome = inferOutcome(orderedPrimitives);
        var constructs = constructExtractor.extract(orderedPrimitives);
        var context = inferContext(orderedPrimitives);

        return new FlowTrajectory(
                sessionId,
                flowId,
                startedAt,
                finishedAt,
                outcome,
                orderedPrimitives,
                constructs,
                context
        );
    }

    private Instant inferFinishedAt(List<BehavioralPrimitive> primitives) {
        return primitives.stream()
                .filter(primitive -> isTerminal(primitive.type()))
                .map(BehavioralPrimitive::timestamp)
                .findFirst()
                .orElse(null);
    }

    private TrajectoryOutcomeType inferOutcome(List<BehavioralPrimitive> primitives) {
        var types = primitives.stream()
                .map(BehavioralPrimitive::type)
                .toList();

        if (types.contains(BehavioralPrimitiveType.COMPLETE)) {
            return TrajectoryOutcomeType.COMPLETED;
        }

        if (types.contains(BehavioralPrimitiveType.ABANDON)) {
            return TrajectoryOutcomeType.ABANDONED;
        }

        if (types.contains(BehavioralPrimitiveType.TIMEOUT)) {
            return TrajectoryOutcomeType.TIMED_OUT;
        }

        if (types.contains(BehavioralPrimitiveType.HUMAN_HANDOFF)
                || types.contains(BehavioralPrimitiveType.TRANSFER)) {
            return TrajectoryOutcomeType.TRANSFERRED;
        }

        return TrajectoryOutcomeType.OPEN;
    }

    private boolean isTerminal(BehavioralPrimitiveType type) {
        return type == BehavioralPrimitiveType.COMPLETE
                || type == BehavioralPrimitiveType.ABANDON
                || type == BehavioralPrimitiveType.TIMEOUT
                || type == BehavioralPrimitiveType.HUMAN_HANDOFF
                || type == BehavioralPrimitiveType.TRANSFER;
    }

    private Map<String, String> inferContext(List<BehavioralPrimitive> primitives) {
        // Por enquanto, mantemos vazio.
        // Depois dá para consolidar metadata das primitives aqui.
        return Map.of();
    }
}
