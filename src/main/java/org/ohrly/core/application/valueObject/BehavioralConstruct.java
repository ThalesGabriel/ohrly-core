package org.ohrly.core.application.valueObject;

import org.ohrly.core.application.type.BehavioralConstructType;
import org.ohrly.core.application.type.BehavioralPrimitiveType;

import java.util.List;

public record BehavioralConstruct(
        BehavioralConstructType type,
        List<BehavioralPrimitiveType> primitives,
        double score,
        String description
) {}
