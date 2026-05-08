package org.ohrly.core.services;

import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class BehaviorPrecedenceService {

    private static final double MATCH_THRESHOLD = 0.75;

    public BehaviorPrecedence analyze(
            List<DailyFlowMetric> metrics,
            FlowBaseline baseline,
            List<BehaviorPattern> historicalPatterns,
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

        int lookbackPeriods = policy.lookbackPeriods();

        if (metrics == null ||
                metrics.size() < lookbackPeriods ||
                historicalPatterns == null ||
                historicalPatterns.isEmpty() ||
                baseline.expectedValue() <= 0) {
            return noMatch(
                    baseline.context(),
                    baseline.metricName()
            );
        }

        List<DailyFlowMetric> ordered = metrics.stream()
                .sorted(Comparator.comparing(DailyFlowMetric::date))
                .toList();

        List<DailyFlowMetric> latestWindow = ordered.subList(
                ordered.size() - lookbackPeriods,
                ordered.size()
        );

        List<Double> currentRatios = latestWindow.stream()
                .map(metric -> metric.averageValue() / baseline.expectedValue())
                .toList();

        double bestScore = historicalPatterns.stream()
                .filter(pattern -> pattern.context().equals(baseline.context()))
                .filter(pattern -> Objects.equals(pattern.metricName(), baseline.metricName()))
                .mapToDouble(pattern -> similarity(
                        currentRatios,
                        pattern.ratiosBeforeCritical()
                ))
                .max()
                .orElse(0);

        boolean matches = bestScore >= MATCH_THRESHOLD;

        String message = matches
                ? String.format(
                "O comportamento atual da métrica %s é semelhante a padrões históricos que precederam degradação crítica. Similaridade: %.2f.",
                baseline.metricName(),
                bestScore
        )
                : String.format(
                "Nenhum padrão histórico crítico semelhante foi encontrado para a métrica %s. Similaridade máxima: %.2f.",
                baseline.metricName(),
                bestScore
        );

        return new BehaviorPrecedence(
                baseline.context(),
                baseline.metricName(),
                matches,
                bestScore,
                message
        );
    }

    private double similarity(
            List<Double> current,
            List<Double> historical
    ) {
        if (current.size() != historical.size()) {
            return 0;
        }

        double totalError = 0;

        for (int i = 0; i < current.size(); i++) {
            totalError += Math.abs(current.get(i) - historical.get(i));
        }

        double meanError = totalError / current.size();

        return Math.max(0, 1 - meanError);
    }

    private BehaviorPrecedence noMatch(
            FlowContext context,
            String metricName
    ) {
        return new BehaviorPrecedence(
                context,
                metricName,
                false,
                0,
                "Sem histórico suficiente para avaliar precedência comportamental."
        );
    }
}