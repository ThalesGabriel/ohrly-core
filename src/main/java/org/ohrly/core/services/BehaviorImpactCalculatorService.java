package org.ohrly.core.services;

import org.ohrly.core.enums.BehaviorStateType;
import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class BehaviorImpactCalculatorService {

    public BehaviorImpact calculate(
            List<DailyFlowMetric> metrics,
            double expectedValue,
            BehaviorStateType state,
            FlowBehaviorPolicy policy,
            String metricName
    ) {
        if (state == BehaviorStateType.NORMAL) {
            return BehaviorImpact.empty(metricName);
        }

        BehaviorThresholds thresholds = policy.thresholds();

        double threshold = thresholdFor(state, expectedValue, thresholds);

        int durationPeriods = 0;
        int impactedSessions = 0;
        double excessValue = 0;
        double impactedBusinessValue = 0;

        LocalDate expectedDate = null;

        for (int i = metrics.size() - 1; i >= 0; i--) {
            DailyFlowMetric metric = metrics.get(i);

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
                    (metric.averageValue() - expectedValue) * metric.count();

            impactedBusinessValue += metric.totalBusinessValue();

            if (policy.requireConsecutiveness()) {
                expectedDate = previousExpectedDate(metric.date(), metric.context());
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

    private double thresholdFor(
            BehaviorStateType state,
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

    private LocalDate previousExpectedDate(LocalDate date, FlowContext context) {
        Object dayType = context.dimensions().get("dayType");

        if ("BUSINESS_DAY".equals(String.valueOf(dayType))) {
            LocalDate previous = date.minusDays(1);

            while (previous.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    previous.getDayOfWeek() == DayOfWeek.SUNDAY) {
                previous = previous.minusDays(1);
            }

            return previous;
        }

        return date.minusDays(1);
    }
}