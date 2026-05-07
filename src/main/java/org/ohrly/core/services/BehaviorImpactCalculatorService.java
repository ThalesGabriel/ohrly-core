package org.ohrly.core.services;

import org.ohrly.core.enums.BehaviorState;
import org.ohrly.core.enums.DayType;
import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class BehaviorImpactCalculatorService {

    public BehaviorImpact calculate(
            List<DailyContextMetric> metrics,
            double expectedAverage,
            BehaviorState state,
            FlowBehaviorPolicy policy
    ) {
        if (state == BehaviorState.NORMAL) {
            return BehaviorImpact.empty();
        }

        BehaviorThresholds thresholds = policy.thresholds();

        double threshold = thresholdFor(state, expectedAverage, thresholds);

        int durationDays = 0;
        int impactedOrders = 0;
        double excessApprovalMinutes = 0;
        double impactedPaymentValue = 0;

        LocalDate expectedDate = null;

        for (int i = metrics.size() - 1; i >= 0; i--) {
            DailyContextMetric metric = metrics.get(i);

            if (policy.requireConsecutiveness()
                    && expectedDate != null
                    && !metric.date().equals(expectedDate)) {
                break;
            }

            if (metric.averageApprovalTime() < threshold) {
                break;
            }

            durationDays++;
            impactedOrders += metric.count();
            excessApprovalMinutes +=
                    (metric.averageApprovalTime() - expectedAverage) * metric.count();
            impactedPaymentValue += metric.totalPaymentValue();

            expectedDate = previousExpectedDate(metric.date(), metric.context());
        }

        if (impactedOrders < policy.minimumVolume()) {
            return BehaviorImpact.empty();
        }

        return new BehaviorImpact(
                durationDays,
                impactedOrders,
                excessApprovalMinutes,
                impactedPaymentValue
        );
    }

    private double thresholdFor(
            BehaviorState state,
            double expectedAverage,
            BehaviorThresholds thresholds
    ) {
        return switch (state) {
            case PRE_INCIDENT ->
                    expectedAverage * thresholds.preIncidentMultiplier();

            case SUSTAINED_DEGRADATION ->
                    expectedAverage * thresholds.degradationMultiplier();

            case ATTENTION ->
                    expectedAverage * thresholds.attentionMultiplier();

            default -> Double.MAX_VALUE;
        };
    }

    private LocalDate previousExpectedDate(LocalDate date, Context context) {
        if (context.dayType() == DayType.BUSINESS_DAY) {
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