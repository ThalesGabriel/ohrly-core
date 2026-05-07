package org.ohrly.core.services;

import org.ohrly.core.enums.BehaviorState;
import org.ohrly.core.valueObjects.*;
import org.springframework.stereotype.Service;

@Service
public class BehaviorKpiMessageBuilderService {

    public String build(
            Context context,
            BehaviorState state,
            double expectedAverage,
            double currentAverage,
            double deviationRatio,
            BehaviorImpact impact,
            BehaviorPrecedence precedence
    ) {
        String baseMessage;

        if (state == BehaviorState.NORMAL) {
            baseMessage = String.format(
                    "O fluxo no contexto %s está operando dentro do comportamento esperado. " +
                            "Tempo médio atual de %.2f min em relação ao esperado de %.2f min.",
                    context,
                    currentAverage,
                    expectedAverage
            );
        } else {
            baseMessage = String.format(
                    "O fluxo no contexto %s está em estado %s: há %d dia(s), " +
                            "o tempo médio está %.2fx acima do esperado (%.2f min vs %.2f min), " +
                            "impactando %d pedidos e acumulando %.2f minutos excedentes.",
                    context,
                    state,
                    impact.durationDays(),
                    deviationRatio,
                    currentAverage,
                    expectedAverage,
                    impact.impactedOrders(),
                    impact.excessApprovalMinutes()
            );
        }

        if (shouldShowPrecedence(state, precedence)) {
            return baseMessage + String.format(
                    " Esse comportamento já ocorreu anteriormente neste contexto e precedeu degradação crítica (similaridade: %.2f).",
                    precedence.similarityScore()
            );
        }

        return baseMessage;
    }

    private boolean shouldShowPrecedence(
            BehaviorState state,
            BehaviorPrecedence precedence
    ) {
        return precedence != null &&
                precedence.matchesHistoricalPattern() &&
                state != BehaviorState.NORMAL;
    }
}