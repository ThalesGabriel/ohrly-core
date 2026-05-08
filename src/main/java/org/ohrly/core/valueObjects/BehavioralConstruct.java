package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.BehavioralPrimitiveType;

import java.util.List;

public record BehavioralConstruct(
        BehavioralConstructType type,
        List<BehavioralPrimitiveType> primitives,
        double score,
        String description
) {}
