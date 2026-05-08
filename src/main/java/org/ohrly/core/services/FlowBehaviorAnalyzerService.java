package org.ohrly.core.services;

import org.ohrly.core.enums.BehaviorStateType;
import org.ohrly.core.enums.DayType;
import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class FlowBehaviorAnalyzerService {

    public BehaviorStateType analyze(
            List<DailyFlowMetric> metrics,
            FlowBaseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (metrics == null || metrics.isEmpty() || baseline == null) {
            return BehaviorStateType.NORMAL;
        }

        if (policy == null) {
            policy = FlowBehaviorPolicy.defaultFor(
                    baseline.context().flowId(),
                    baseline.context().flowId()
            );
        }

        BehaviorThresholds thresholds = policy.thresholds();

        List<DailyFlowMetric> orderedMetrics = metrics.stream()
                .sorted(Comparator.comparing(DailyFlowMetric::date))
                .toList();

        DailyFlowMetric latest = orderedMetrics.getLast();

        double latestAverage = latest.averageValue();
        double expectedAverage = baseline.expectedValue();

        if (expectedAverage <= 0) {
            return BehaviorStateType.NORMAL;
        }

        if (latestAverage <= expectedAverage) {
            return BehaviorStateType.NORMAL;
        }

        if (hasActivePreIncident(orderedMetrics, expectedAverage, policy)) {
            return BehaviorStateType.PRE_INCIDENT;
        }

        if (hasActiveSustainedDegradation(orderedMetrics, expectedAverage, policy)) {
            return BehaviorStateType.SUSTAINED_DEGRADATION;
        }

        if (latestAverage >= expectedAverage * thresholds.attentionMultiplier()) {
            return BehaviorStateType.ATTENTION;
        }

        return BehaviorStateType.NORMAL;
    }

    private boolean hasActivePreIncident(
            List<DailyFlowMetric> metrics,
            double expectedAverage,
            FlowBehaviorPolicy policy
    ) {
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
                expectedDate = previousExpectedDate(metric.date(), metric.context());
            }

            if (consecutiveDays >= thresholds.preIncidentDays()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasActiveSustainedDegradation(
            List<DailyFlowMetric> metrics,
            double expectedAverage,
            FlowBehaviorPolicy policy
    ) {
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

            boolean degraded =
                    metric.averageValue() >= expectedAverage * thresholds.degradationMultiplier();

            if (!degraded) {
                break;
            }

            consecutiveDays++;

            if (policy.requireConsecutiveness()) {
                expectedDate = previousExpectedDate(metric.date(), metric.context());
            }

            if (consecutiveDays >= thresholds.sustainedDegradationDays()) {
                return true;
            }
        }

        return false;
    }

    private LocalDate previousExpectedDate(LocalDate date, FlowContext context) {
        Object dayType = context.dimensions().get("dayType");

        if (DayType.BUSINESS_DAY.name().equals(String.valueOf(dayType))) {
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