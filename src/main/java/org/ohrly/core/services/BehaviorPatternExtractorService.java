package org.ohrly.core.services;

import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BehaviorPatternExtractorService {

    public List<BehaviorPattern> extract(
            List<DailyContextMetric> metrics,
            Baseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (baseline == null) {
            return List.of();
        }

        if (policy == null) {
            policy = FlowBehaviorPolicy.defaultFor(
                    baseline.context().toString(),
                    baseline.context().toString()
            );
        }

        BehaviorThresholds thresholds = policy.thresholds();

        int lookbackDays = policy.lookbackPeriods();
        double criticalMultiplier = thresholds.preIncidentMultiplier();

        if (metrics == null || metrics.size() < lookbackDays + 1 || baseline.average() <= 0) {
            return List.of();
        }

        List<DailyContextMetric> ordered = metrics.stream()
                .sorted(Comparator.comparing(DailyContextMetric::date))
                .toList();

        List<BehaviorPattern> patterns = new ArrayList<>();

        for (int i = lookbackDays; i < ordered.size(); i++) {
            DailyContextMetric current = ordered.get(i);

            boolean critical = current.averageApprovalTime() >= baseline.average() * criticalMultiplier;

            if (!critical) {
                continue;
            }

            List<Double> ratios = new ArrayList<>();

            for (int j = i - lookbackDays; j < i; j++) {
                DailyContextMetric previous = ordered.get(j);
                ratios.add(previous.averageApprovalTime() / baseline.average());
            }

            patterns.add(new BehaviorPattern(
                    baseline.context(),
                    current.date(),
                    ratios
            ));
        }

        return patterns;
    }
}
