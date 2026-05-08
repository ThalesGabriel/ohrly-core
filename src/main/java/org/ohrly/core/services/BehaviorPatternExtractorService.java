package org.ohrly.core.services;

import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BehaviorPatternExtractorService {

    public List<BehaviorPattern> extract(
            List<DailyFlowMetric> metrics,
            FlowBaseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (baseline == null) {
            return List.of();
        }

        if (policy == null) {
            policy = FlowBehaviorPolicy.defaultFor(
                    baseline.context().flowId(),
                    baseline.context().flowId()
            );
        }

        BehaviorThresholds thresholds = policy.thresholds();

        int lookbackPeriods = policy.lookbackPeriods();
        double criticalMultiplier = thresholds.preIncidentMultiplier();

        if (metrics == null ||
                metrics.size() < lookbackPeriods + 1 ||
                baseline.expectedValue() <= 0) {
            return List.of();
        }

        List<DailyFlowMetric> ordered = metrics.stream()
                .sorted(Comparator.comparing(DailyFlowMetric::date))
                .toList();

        List<BehaviorPattern> patterns = new ArrayList<>();

        for (int i = lookbackPeriods; i < ordered.size(); i++) {
            DailyFlowMetric current = ordered.get(i);

            boolean critical =
                    current.averageValue() >= baseline.expectedValue() * criticalMultiplier;

            if (!critical) {
                continue;
            }

            List<Double> ratios = new ArrayList<>();

            for (int j = i - lookbackPeriods; j < i; j++) {
                DailyFlowMetric previous = ordered.get(j);
                ratios.add(previous.averageValue() / baseline.expectedValue());
            }

            patterns.add(new BehaviorPattern(
                    baseline.context(),
                    baseline.metricName(),
                    current.date(),
                    ratios
            ));
        }

        return patterns;
    }
}
