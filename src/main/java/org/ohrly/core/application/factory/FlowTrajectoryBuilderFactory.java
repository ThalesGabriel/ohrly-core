package org.ohrly.core.application.factory;

import org.ohrly.core.application.service.FlowTrajectoryAwareBehavioralConstructExtractorService;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.application.type.FlowOutcomeType;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class FlowTrajectoryBuilderFactory {

    @Autowired
    private FlowTrajectoryAwareBehavioralConstructExtractorService constructExtractor;

    public FlowTrajectory build(
            String flowId,
            String sessionId,
            Instant startedAt,
            Instant endedAt,
            List<BehavioralPrimitive> primitives,
            FlowContext context
    ) {

        return new FlowTrajectory(
                sessionId,
                flowId,
                startedAt,
                endedAt,
                resolveOutcome(primitives),
                primitives,
                constructExtractor.extract(primitives),
                context.dimensions()
        );
    }

    private FlowOutcomeType resolveOutcome(
            List<BehavioralPrimitive> primitives
    ) {
        if (has(primitives, BehavioralPrimitiveType.COMPLETE)) {
            return FlowOutcomeType.COMPLETED;
        }

        if (has(primitives, BehavioralPrimitiveType.TIMEOUT)) {
            return FlowOutcomeType.TIMED_OUT;
        }

        if (has(primitives, BehavioralPrimitiveType.HUMAN_HANDOFF)) {
            return FlowOutcomeType.TRANSFERRED;
        }

        if (has(primitives, BehavioralPrimitiveType.ABANDON)) {
            return FlowOutcomeType.ABANDONED;
        }

        return FlowOutcomeType.OPEN;
    }

    private boolean has(
            List<BehavioralPrimitive> primitives,
            BehavioralPrimitiveType type
    ) {
        return primitives.stream()
                .anyMatch(primitive -> primitive.type() == type);
    }
}
