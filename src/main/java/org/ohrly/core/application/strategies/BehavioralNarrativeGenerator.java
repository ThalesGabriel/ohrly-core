package org.ohrly.core.application.strategies;

import org.ohrly.core.application.valueObject.BehavioralDriftResult;

public interface BehavioralNarrativeGenerator {

    String generate(BehavioralDriftResult result);
}