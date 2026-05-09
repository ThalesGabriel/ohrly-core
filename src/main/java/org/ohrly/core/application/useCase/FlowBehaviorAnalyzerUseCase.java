package org.ohrly.core.application.useCase;

import org.ohrly.core.application.strategies.BehavioralStateEvaluator;
import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class FlowBehaviorAnalyzerUseCase {

    @Autowired
    private List<BehavioralStateEvaluator> evaluators;

    public BehavioralStateType analyze(
            List<DailyFlowMetric> metrics,
            FlowBaseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (metrics == null || metrics.isEmpty() || baseline == null) {
            return BehavioralStateType.NORMAL;
        }

        FlowBehaviorPolicy effectivePolicy = policy != null
                ? policy
                : FlowBehaviorPolicy.defaultFor(
                baseline.context().flowId(),
                baseline.context().flowId()
        );

        List<DailyFlowMetric> orderedMetrics = metrics.stream()
                .sorted(Comparator.comparing(DailyFlowMetric::date))
                .toList();

        return evaluators.stream()
                .filter(evaluator -> evaluator.matches(
                        orderedMetrics,
                        baseline,
                        effectivePolicy
                ))
                .map(BehavioralStateEvaluator::state)
                .findFirst()
                .orElse(BehavioralStateType.NORMAL);
    }

}
