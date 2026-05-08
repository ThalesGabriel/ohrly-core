package org.ohrly.core;

import org.ohrly.core.valueObjects.BehavioralDriftResult;

public interface BehavioralNarrativeGenerator {

    String generate(BehavioralDriftResult result);
}