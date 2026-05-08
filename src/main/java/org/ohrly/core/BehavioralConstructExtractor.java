package org.ohrly.core;

import org.ohrly.core.valueObjects.BehavioralConstruct;
import org.ohrly.core.valueObjects.BehavioralPrimitive;

import java.util.List;

public interface BehavioralConstructExtractor {
    List<BehavioralConstruct> extract(List<BehavioralPrimitive> primitives);
}
