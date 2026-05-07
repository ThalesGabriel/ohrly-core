package org.ohrly.core.services;

import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class BehaviorPrecedenceService {

    private static final double MATCH_THRESHOLD = 0.75;

    public BehaviorPrecedence analyze(List<DailyContextMetric> metrics,
                                      Baseline baseline,
                                      List<BehaviorPattern> historicalPatterns,
                                      FlowBehaviorPolicy policy) {
        if (baseline == null) {
            throw new IllegalArgumentException("Baseline cannot be null");
        }

        if (policy == null) {
            policy = FlowBehaviorPolicy.defaultFor(
                    baseline.context().toString(),
                    baseline.context().toString()
            );
        }

        int lookbackPeriods = policy.lookbackPeriods();

        if (metrics == null ||
                metrics.size() < lookbackPeriods ||
                historicalPatterns == null ||
                historicalPatterns.isEmpty() ||
                baseline.average() <= 0) {
            return noMatch(baseline.context());
        }

        List<DailyContextMetric> ordered = metrics.stream()
                .sorted(Comparator.comparing(DailyContextMetric::date))
                .toList();

        List<DailyContextMetric> latestWindow = ordered.subList(
                ordered.size() - lookbackPeriods,
                ordered.size()
        );

        List<Double> currentRatios = latestWindow.stream()
                .map(metric -> metric.averageApprovalTime() / baseline.average())
                .toList();

        double bestScore = historicalPatterns.stream()
                .mapToDouble(pattern -> similarity(
                        currentRatios,
                        pattern.ratiosBeforeCritical()
                ))
                .max()
                .orElse(0);

        boolean matches = bestScore >= MATCH_THRESHOLD;

        String message = matches
                ? String.format(
                "O comportamento atual é semelhante a padrões históricos que precederam degradação crítica. Similaridade: %.2f.",
                bestScore
        )
                : String.format(
                "Nenhum padrão histórico crítico semelhante foi encontrado. Similaridade máxima: %.2f.",
                bestScore
        );

        return new BehaviorPrecedence(
                baseline.context(),
                matches,
                bestScore,
                message
        );
    }

    private double similarity(List<Double> current, List<Double> historical) {
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

    private BehaviorPrecedence noMatch(Context context) {
        return new BehaviorPrecedence(
                context,
                false,
                0,
                "Sem histórico suficiente para avaliar precedência comportamental."
        );
    }
}