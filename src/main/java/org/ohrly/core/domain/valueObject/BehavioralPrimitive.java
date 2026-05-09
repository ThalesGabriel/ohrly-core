package org.ohrly.core.domain.valueObject;

import org.ohrly.core.application.type.BehavioralPrimitiveType;

import java.time.Instant;

public record BehavioralPrimitive(
        BehavioralPrimitiveType type,
        String sessionId,
        String step,
        Instant timestamp
) {

    public static BehavioralPrimitive of(
            BehavioralPrimitiveType type,
            String sessionId,
            String step,
            Instant timestamp
    ) {
        return new BehavioralPrimitive(
                type,
                sessionId,
                step,
                timestamp
        );
    }

    public static BehavioralPrimitive of(
            BehavioralPrimitiveType type,
            String sessionId,
            Instant timestamp
    ) {
        return new BehavioralPrimitive(
                type,
                sessionId,
                null,
                timestamp
        );
    }
}
