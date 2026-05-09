package org.ohrly.core.application.service;

import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.type.FlowDayType;
import org.ohrly.core.application.utils.PreviousExpectedDateUtils;
import org.ohrly.core.application.valueObject.BehaviorImpact;
import org.ohrly.core.application.valueObject.BehaviorThresholds;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class BehaviorImpactCalculatorService {

    public BehaviorImpact calculate(
            List<DailyFlowMetric> metrics,
            double expectedValue,
            BehavioralStateType state,
            FlowBehaviorPolicy policy,
            String metricName
    ) {
        if (state == BehavioralStateType.NORMAL ||
                metrics == null ||
                metrics.isEmpty() ||
                expectedValue <= 0 ||
                policy == null) {
            return BehaviorImpact.empty(metricName);
        }

        List<DailyFlowMetric> orderedMetrics = metrics.stream()
                .sorted(Comparator.comparing(DailyFlowMetric::date))
                .toList();

        BehaviorThresholds thresholds = policy.thresholds();
        double threshold = thresholdFor(state, expectedValue, thresholds);

        int durationPeriods = 0;
        int impactedSessions = 0;
        double excessValue = 0;
        double impactedBusinessValue = 0;

        LocalDate expectedDate = null;

        for (int i = orderedMetrics.size() - 1; i >= 0; i--) {
            DailyFlowMetric metric = orderedMetrics.get(i);

            if (policy.requireConsecutiveness()
                    && expectedDate != null
                    && !metric.date().equals(expectedDate)) {
                break;
            }

            if (metric.averageValue() < threshold) {
                break;
            }

            durationPeriods++;
            impactedSessions += metric.count();

            excessValue +=
                    Math.max(0, metric.averageValue() - expectedValue)
                            * metric.count();

            impactedBusinessValue += metric.totalBusinessValue();

            if (policy.requireConsecutiveness()) {
                expectedDate = PreviousExpectedDateUtils.calculate(
                        metric.date(),
                        resolveDayType(metric)
                );
            }
        }

        if (impactedSessions < policy.minimumVolume()) {
            return BehaviorImpact.empty(metricName);
        }

        return new BehaviorImpact(
                durationPeriods,
                impactedSessions,
                excessValue,
                impactedBusinessValue,
                metricName
        );
    }

    private FlowDayType resolveDayType(DailyFlowMetric metric) {
        Object value = metric.context().dimensions().get("dayType");

        if (value instanceof FlowDayType dayType) {
            return dayType;
        }

        return FlowDayType.valueOf(String.valueOf(value));
    }

    private double thresholdFor(
            BehavioralStateType state,
            double expectedValue,
            BehaviorThresholds thresholds
    ) {
        return switch (state) {
            case PRE_INCIDENT ->
                    expectedValue * thresholds.preIncidentMultiplier();

            case SUSTAINED_DEGRADATION ->
                    expectedValue * thresholds.degradationMultiplier();

            case ATTENTION ->
                    expectedValue * thresholds.attentionMultiplier();

            default -> Double.MAX_VALUE;
        };
    }

}