package org.ohrly.core.application.strategies;

import org.ohrly.core.application.valueObject.BehavioralDriftResult;
import org.ohrly.core.application.valueObject.FlowBehaviorWindow;

public interface BehavioralDriftAnalyzer {

    BehavioralDriftResult analyze(
            FlowBehaviorWindow baseline,
            FlowBehaviorWindow current
    );
}
