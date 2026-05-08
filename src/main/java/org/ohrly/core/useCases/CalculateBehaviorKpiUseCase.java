package org.ohrly.core.useCases;

import org.ohrly.core.enums.BehaviorStateType;
import org.ohrly.core.services.*;
import org.ohrly.core.valueObjects.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CalculateBehaviorKpiUseCase {

    @Autowired
    private FlowBehaviorAnalyzerService analyzer;

    @Autowired
    private BehaviorPatternExtractorService patternExtractor;

    @Autowired
    private BehaviorPrecedenceService precedenceService;

    @Autowired
    private BehaviorImpactCalculatorService impactCalculator;

    @Autowired
    private BehaviorKpiMessageBuilderService messageBuilder;

    public BehaviorKpi calculate(
            List<DailyFlowMetric> metrics,
            FlowBaseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (baseline == null) {
            throw new IllegalArgumentException("Baseline cannot be null");
        }

        if (policy == null) {
            policy = FlowBehaviorPolicy.defaultFor(
                    baseline.context().flowId(),
                    baseline.context().flowId()
            );
        }

        if (metrics == null || metrics.isEmpty()) {
            BehaviorPrecedence precedence = BehaviorPrecedence.empty(
                    baseline.context(),
                    baseline.metricName(),
                    "Sem histórico suficiente para avaliar precedência comportamental."
            );

            return BehaviorKpi.empty(
                    baseline.context(),
                    baseline.metricName(),
                    baseline.expectedValue(),
                    "Sem métricas suficientes para análise.",
                    precedence
            );
        }

        List<DailyFlowMetric> orderedMetrics = metrics.stream()
                .sorted(Comparator.comparing(DailyFlowMetric::date))
                .toList();

        BehaviorStateType state = analyzer.analyze(
                orderedMetrics,
                baseline,
                policy
        );

        DailyFlowMetric latest = orderedMetrics.getLast();

        double expectedValue = baseline.expectedValue();
        double currentValue = latest.averageValue();

        double deviationValue = currentValue - expectedValue;

        double deviationRatio = expectedValue > 0
                ? currentValue / expectedValue
                : 0;

        BehaviorImpact impact = impactCalculator.calculate(
                orderedMetrics,
                expectedValue,
                state,
                policy,
                baseline.metricName()
        );

        List<BehaviorPattern> historicalPatterns = patternExtractor.extract(
                orderedMetrics,
                baseline,
                policy
        );

        BehaviorPrecedence precedence = precedenceService.analyze(
                orderedMetrics,
                baseline,
                historicalPatterns,
                policy
        );

        String message = messageBuilder.build(
                baseline.context(),
                baseline.metricName(),
                state,
                expectedValue,
                currentValue,
                deviationRatio,
                impact,
                precedence
        );

        return new BehaviorKpi(
                baseline.context(),
                baseline.metricName(),
                state,
                expectedValue,
                currentValue,
                deviationValue,
                deviationRatio,
                impact.durationDays(),
                impact.impactedEvents(),
                impact.excessValue(),
                message,
                precedence,
                impact.impactedValue()
        );
    }
}
