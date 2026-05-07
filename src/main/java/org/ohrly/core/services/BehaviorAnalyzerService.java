package org.ohrly.core.services;

import org.ohrly.core.enums.BehaviorState;
import org.ohrly.core.enums.DayType;
import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class BehaviorAnalyzerService {

    public BehaviorState analyze(
            List<DailyContextMetric> metrics,
            Baseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (metrics == null || metrics.isEmpty() || baseline == null) {
            return BehaviorState.NORMAL;
        }

        if (policy == null) {
            policy = FlowBehaviorPolicy.defaultFor(
                    baseline.context().toString(),
                    baseline.context().toString()
            );
        }

        BehaviorThresholds thresholds = policy.thresholds();

        List<DailyContextMetric> orderedMetrics = metrics.stream()
                .sorted(Comparator.comparing(DailyContextMetric::date))
                .toList();

        DailyContextMetric latest = orderedMetrics.getLast();

        double latestAverage = latest.averageApprovalTime();
        double expectedAverage = baseline.average();

        if (expectedAverage <= 0) {
            return BehaviorState.NORMAL;
        }

        // Se o estado atual está igual ou melhor que o esperado,
        // não existe degradação ativa agora.
        if (latestAverage <= expectedAverage) {
            return BehaviorState.NORMAL;
        }

        if (hasActivePreIncident(orderedMetrics, expectedAverage, policy)) {
            return BehaviorState.PRE_INCIDENT;
        }

        if (hasActiveSustainedDegradation(orderedMetrics, expectedAverage, policy)) {
            return BehaviorState.SUSTAINED_DEGRADATION;
        }

        if (latestAverage >= expectedAverage * thresholds.attentionMultiplier()) {
            return BehaviorState.ATTENTION;
        }

        return BehaviorState.NORMAL;
    }

    private boolean hasActivePreIncident(
            List<DailyContextMetric> metrics,
            double expectedAverage,
            FlowBehaviorPolicy policy
    ) {
        BehaviorThresholds thresholds = policy.thresholds();

        int consecutiveDays = 0;
        LocalDate expectedDate = null;

        for (int i = metrics.size() - 1; i >= 0; i--) {
            DailyContextMetric metric = metrics.get(i);

            if (policy.requireConsecutiveness()
                    && expectedDate != null
                    && !metric.date().equals(expectedDate)) {
                break;
            }

            boolean preIncident =
                    metric.averageApprovalTime() >= expectedAverage * thresholds.preIncidentMultiplier();

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
            List<DailyContextMetric> metrics,
            double expectedAverage,
            FlowBehaviorPolicy policy
    ) {
        BehaviorThresholds thresholds = policy.thresholds();

        int consecutiveDays = 0;
        LocalDate expectedDate = null;

        for (int i = metrics.size() - 1; i >= 0; i--) {
            DailyContextMetric metric = metrics.get(i);

            if (policy.requireConsecutiveness()
                    && expectedDate != null
                    && !metric.date().equals(expectedDate)) {
                break;
            }

            boolean degraded = metric.averageApprovalTime() >= expectedAverage * thresholds.degradationMultiplier();

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

    private LocalDate previousExpectedDate(LocalDate date, Context context) {
        if (DayType.BUSINESS_DAY.equals(context.dayType())) {
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