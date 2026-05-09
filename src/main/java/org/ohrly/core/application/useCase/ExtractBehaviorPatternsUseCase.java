package org.ohrly.core.application.useCase;

import org.ohrly.core.application.service.CriticalBehaviorDetectorService;
import org.ohrly.core.application.service.FlowBehaviorPolicyResolverService;
import org.ohrly.core.application.valueObject.BehaviorPattern;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ExtractBehaviorPatternsUseCase {

    @Autowired
    private FlowBehaviorPolicyResolverService policyResolver;

    @Autowired
    private CriticalBehaviorDetectorService criticalDetector;

    public List<BehaviorPattern> extract(
            List<DailyFlowMetric> metrics,
            FlowBaseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (baseline == null || metrics == null) {
            return List.of();
        }

        FlowBehaviorPolicy effectivePolicy =
                policyResolver.resolve(policy, baseline);

        int lookbackPeriods = effectivePolicy.lookbackPeriods();

        if (metrics.size() < lookbackPeriods + 1 ||
                baseline.expectedValue() <= 0) {
            return List.of();
        }

        List<DailyFlowMetric> ordered = metrics.stream()
                .sorted(Comparator.comparing(DailyFlowMetric::date))
                .toList();

        List<BehaviorPattern> patterns = new ArrayList<>();

        for (int i = lookbackPeriods; i < ordered.size(); i++) {
            DailyFlowMetric current = ordered.get(i);

            if (!criticalDetector.isCritical(
                    current,
                    baseline,
                    effectivePolicy
            )) {
                continue;
            }

            patterns.add(buildPattern(
                    ordered,
                    i,
                    lookbackPeriods,
                    baseline
            ));
        }

        return patterns;
    }

    private BehaviorPattern buildPattern(
            List<DailyFlowMetric> ordered,
            int criticalIndex,
            int lookbackPeriods,
            FlowBaseline baseline
    ) {
        List<Double> ratios = new ArrayList<>();

        for (int j = criticalIndex - lookbackPeriods; j < criticalIndex; j++) {
            ratios.add(
                    ordered.get(j).averageValue() / baseline.expectedValue()
            );
        }

        DailyFlowMetric criticalMetric = ordered.get(criticalIndex);

        return new BehaviorPattern(
                baseline.context(),
                baseline.metricName(),
                criticalMetric.date(),
                ratios
        );
    }
}