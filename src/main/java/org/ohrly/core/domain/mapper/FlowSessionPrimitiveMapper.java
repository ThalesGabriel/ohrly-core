package org.ohrly.core.domain.mapper;

import org.ohrly.core.domain.entities.FlowEvent;
import org.ohrly.core.domain.entities.FlowSession;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FlowSessionPrimitiveMapper {

    public List<BehavioralPrimitive> map(FlowSession session) {
        List<BehavioralPrimitive> primitives = new ArrayList<>();

        primitives.add(
                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.START,
                        session.getSessionId(),
                        session.getStartedAt()
                )
        );

        session.getEvents().stream()
                .sorted(Comparator.comparing(FlowEvent::getOccurredAt))
                .map(event -> mapEvent(session, event))
                .forEach(primitives::add);

        if (session.isClosed() || session.getEndReason() != null) {
            primitives.add(
                    BehavioralPrimitive.of(
                            mapSessionEnd(session),
                            session.getSessionId(),
                            session.getEndedAt()
                    )
            );
        }

        return primitives;
    }

    private BehavioralPrimitive mapEvent(
            FlowSession session,
            FlowEvent event
    ) {
        return BehavioralPrimitive.of(
                BehavioralPrimitiveType.STEP_COMPLETED,
                session.getSessionId(),
                event.getStepName(),
                event.getOccurredAt()
        );
    }

    private BehavioralPrimitiveType mapSessionEnd(
            FlowSession session
    ) {
        return switch (session.getEndReason()) {
            case SUCCESS -> BehavioralPrimitiveType.COMPLETE;
            case TIMEOUT -> BehavioralPrimitiveType.TIMEOUT;
            case HANDOFF -> BehavioralPrimitiveType.HUMAN_HANDOFF;
            default -> BehavioralPrimitiveType.ABANDON;
        };
    }
}