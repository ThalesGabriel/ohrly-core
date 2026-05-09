package org.ohrly.core.application.strategies;

import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;

import java.util.List;

public interface BehavioralStateEvaluator {

    boolean matches(
            List<DailyFlowMetric> metrics,
            FlowBaseline baseline,
            FlowBehaviorPolicy policy
    );

    BehavioralStateType state();
}
