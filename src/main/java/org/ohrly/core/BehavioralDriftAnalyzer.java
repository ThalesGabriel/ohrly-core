package org.ohrly.core;

import org.ohrly.core.valueObjects.BehavioralDriftResult;
import org.ohrly.core.valueObjects.FlowBehaviorWindow;

public interface BehavioralDriftAnalyzer {

    BehavioralDriftResult analyze(
            FlowBehaviorWindow baseline,
            FlowBehaviorWindow current
    );
}
