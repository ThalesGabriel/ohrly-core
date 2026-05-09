package org.ohrly.core.application.factory;

import org.ohrly.core.application.valueObject.BehaviorImpact;
import org.ohrly.core.application.valueObject.BehaviorPrecedence;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.ohrly.core.application.type.BehavioralStateType;
import org.springframework.stereotype.Service;

@Service
public class BehaviorKpiMessageFactory {

    public String build(
            FlowContext context,
            String metricName,
            BehavioralStateType state,
            double expectedValue,
            double currentValue,
            double deviationRatio,
            BehaviorImpact impact,
            BehaviorPrecedence precedence
    ) {
        String baseMessage;

        if (state == BehavioralStateType.NORMAL) {
            baseMessage = String.format(
                    "O fluxo no contexto %s está operando dentro do comportamento esperado para a métrica %s. " +
                            "Valor atual: %.2f; valor esperado: %.2f.",
                    context,
                    metricName,
                    currentValue,
                    expectedValue
            );
        } else {
            baseMessage = String.format(
                    "O fluxo no contexto %s está em estado %s para a métrica %s: há %d período(s), " +
                            "o valor atual está %.2fx acima do esperado (%.2f vs %.2f), " +
                            "impactando %d evento(s) e acumulando %.2f de excesso.",
                    context,
                    state,
                    metricName,
                    impact.durationDays(),
                    deviationRatio,
                    currentValue,
                    expectedValue,
                    impact.impactedEvents(),
                    impact.excessValue()
            );
        }

        if (shouldShowPrecedence(state, precedence)) {
            return baseMessage + String.format(
                    " Esse comportamento da métrica %s já ocorreu anteriormente neste contexto e precedeu degradação crítica (similaridade: %.2f).",
                    metricName,
                    precedence.similarityScore()
            );
        }

        return baseMessage;
    }

    private boolean shouldShowPrecedence(
            BehavioralStateType state,
            BehaviorPrecedence precedence
    ) {
        return precedence != null &&
                precedence.matchesHistoricalPattern() &&
                state != BehavioralStateType.NORMAL;
    }
}