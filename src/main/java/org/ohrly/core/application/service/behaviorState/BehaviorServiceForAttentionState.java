package org.ohrly.core.application.service.behaviorState;

import org.ohrly.core.application.strategies.BehavioralStateEvaluator;
import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.valueObject.BehaviorThresholds;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Order(4)
public class BehaviorServiceForAttentionState implements BehavioralStateEvaluator {

    @Override
    public boolean matches(List<DailyFlowMetric> metrics, FlowBaseline baseline, FlowBehaviorPolicy policy) {
        DailyFlowMetric latest = metrics.getLast();

        BehaviorThresholds thresholds = policy.thresholds();

        double latestAverage = latest.averageValue();
        double expectedAverage = baseline.expectedValue();

        return latestAverage >= expectedAverage * thresholds.attentionMultiplier();
    }

    @Override
    public BehavioralStateType state() {
        return BehavioralStateType.ATTENTION;
    }

}
