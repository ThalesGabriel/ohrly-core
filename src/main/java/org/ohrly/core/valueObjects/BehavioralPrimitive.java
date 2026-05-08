package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehavioralPrimitiveType;

import java.time.Instant;

public record BehavioralPrimitive(
        BehavioralPrimitiveType type,
        String sessionId,
        String step,
        Instant timestamp
) {}
