package org.ohrly.core.application.service;

import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.stereotype.Service;

@Service
public class CriticalBehaviorDetectorService {

    public boolean isCritical(
            DailyFlowMetric metric,
            FlowBaseline baseline,
            FlowBehaviorPolicy policy
    ) {
        return metric.averageValue()
                >= baseline.expectedValue()
                * policy.thresholds().preIncidentMultiplier();
    }
}
