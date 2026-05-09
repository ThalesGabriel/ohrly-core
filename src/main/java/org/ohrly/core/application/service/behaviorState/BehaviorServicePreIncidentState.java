package org.ohrly.core.application.service.behaviorState;

import org.ohrly.core.application.strategies.BehavioralStateEvaluator;
import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.type.FlowDayType;
import org.ohrly.core.application.utils.PreviousExpectedDateUtils;
import org.ohrly.core.application.valueObject.BehaviorThresholds;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Order(2)
public class BehaviorServicePreIncidentState implements BehavioralStateEvaluator {

    @Override
    public boolean matches(List<DailyFlowMetric> metrics, FlowBaseline baseline, FlowBehaviorPolicy policy) {
        double expectedAverage = baseline.expectedValue();
        BehaviorThresholds thresholds = policy.thresholds();

        int consecutiveDays = 0;
        LocalDate expectedDate = null;

        for (int i = metrics.size() - 1; i >= 0; i--) {
            DailyFlowMetric metric = metrics.get(i);

            if (policy.requireConsecutiveness()
                    && expectedDate != null
                    && !metric.date().equals(expectedDate)) {
                break;
            }

            boolean preIncident =
                    metric.averageValue() >= expectedAverage * thresholds.preIncidentMultiplier();

            if (!preIncident) {
                break;
            }

            consecutiveDays++;

            if (policy.requireConsecutiveness()) {
                expectedDate = PreviousExpectedDateUtils
                        .calculate(metric.date(), FlowDayType.resolveDayType(metric));
            }

            if (consecutiveDays >= thresholds.preIncidentDays()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BehavioralStateType state() {
        return BehavioralStateType.PRE_INCIDENT;
    }

}
