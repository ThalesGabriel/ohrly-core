package org.ohrly.core.useCases;

import org.ohrly.core.enums.BehaviorState;
import org.ohrly.core.services.*;
import org.ohrly.core.valueObjects.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CalculateBehaviorKpiUseCase {

    @Autowired
    private BehaviorAnalyzerService analyzer;

    @Autowired
    private BehaviorPatternExtractorService patternExtractor;

    @Autowired
    private BehaviorPrecedenceService precedenceService;

    @Autowired
    private BehaviorImpactCalculatorService impactCalculator;

    @Autowired
    private BehaviorKpiMessageBuilderService messageBuilder;

    public BehaviorKpi calculate(
            List<DailyContextMetric> metrics,
            Baseline baseline,
            FlowBehaviorPolicy policy
    ) {
        if (baseline == null) {
            throw new IllegalArgumentException("Baseline cannot be null");
        }

        if (policy == null) {
            policy = FlowBehaviorPolicy.defaultFor(
                    baseline.context().toString(),
                    baseline.context().toString()
            );
        }

        if (metrics == null || metrics.isEmpty()) {
            BehaviorPrecedence precedence = BehaviorPrecedence.empty(
                    baseline.context(),
                    "Sem histórico suficiente para avaliar precedência comportamental."
            );

            return BehaviorKpi.empty(
                    baseline.context(),
                    baseline.average(),
                    "Sem métricas suficientes para análise.",
                    precedence
            );
        }

        List<DailyContextMetric> orderedMetrics = metrics.stream()
                .sorted(Comparator.comparing(DailyContextMetric::date))
                .toList();

        BehaviorState state = analyzer.analyze(
                orderedMetrics,
                baseline,
                policy
        );

        DailyContextMetric latest = orderedMetrics.get(orderedMetrics.size() - 1);

        double expectedAverage = baseline.average();
        double currentAverage = latest.averageApprovalTime();

        double deviationMinutes = currentAverage - expectedAverage;

        double deviationRatio = expectedAverage > 0
                ? currentAverage / expectedAverage
                : 0;

        BehaviorImpact impact = impactCalculator.calculate(
                orderedMetrics,
                expectedAverage,
                state,
                policy
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
                state,
                expectedAverage,
                currentAverage,
                deviationRatio,
                impact,
                precedence
        );

        return new BehaviorKpi(
                baseline.context(),
                state,
                expectedAverage,
                currentAverage,
                deviationMinutes,
                deviationRatio,
                impact.durationDays(),
                impact.impactedOrders(),
                impact.excessApprovalMinutes(),
                message,
                precedence,
                impact.impactedPaymentValue()
        );
    }
}
