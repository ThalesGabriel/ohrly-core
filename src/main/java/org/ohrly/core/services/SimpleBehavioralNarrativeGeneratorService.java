package org.ohrly.core.services;

import org.ohrly.core.BehavioralNarrativeGenerator;
import org.ohrly.core.valueObjects.BehavioralDriftResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

@Service
public class SimpleBehavioralNarrativeGeneratorService implements BehavioralNarrativeGenerator {

    @Override
    public String generate(BehavioralDriftResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        String strongestSignal = strongestSignal(result.signals());

        return switch (result.state()) {
            case NORMAL -> normalNarrative(result);
            case ATTENTION -> attentionNarrative(result, strongestSignal);
            case DEGRADED -> degradedNarrative(result, strongestSignal);
            case CRITICAL -> criticalNarrative(result, strongestSignal);
        };
    }

    private String normalNarrative(BehavioralDriftResult result) {
        return """
                O fluxo %s permanece dentro do comportamento esperado.

                Não há sinais relevantes de degradação funcional na janela atual.
                """.formatted(result.flowId());
    }

    private String attentionNarrative(
            BehavioralDriftResult result,
            String strongestSignal
    ) {
        return """
                O fluxo %s começou a apresentar sinais iniciais de mudança comportamental.

                O principal sinal observado foi %s.

                Ainda não há degradação severa, mas o comportamento atual já se afastou do baseline saudável.
                """.formatted(
                result.flowId(),
                humanizeSignal(strongestSignal)
        );
    }

    private String degradedNarrative(
            BehavioralDriftResult result,
            String strongestSignal
    ) {
        return """
                O fluxo %s entrou em degradação funcional.

                A trajetória atual está se afastando do comportamento saudável esperado.

                O principal sinal observado foi %s.

                Desde a janela analisada, sessões nesse fluxo passaram a apresentar maior fricção, perda de continuidade ou aumento de esforço implícito.

                Se esse comportamento persistir, há risco de impacto operacional progressivo.
                """.formatted(
                result.flowId(),
                humanizeSignal(strongestSignal)
        );
    }

    private String criticalNarrative(
            BehavioralDriftResult result,
            String strongestSignal
    ) {
        return """
                O fluxo %s apresenta degradação funcional crítica.

                A trajetória atual mudou de forma relevante em relação ao baseline saudável.

                O principal sinal observado foi %s.

                O comportamento sugere perda sustentada de continuidade funcional e maior risco de impacto operacional imediato.

                Se nada mudar, a tendência é aumento de abandono, maior esforço operacional e possível deterioração dos indicadores finais do fluxo.
                """.formatted(
                result.flowId(),
                humanizeSignal(strongestSignal)
        );
    }

    private String strongestSignal(Map<String, Double> signals) {
        if (signals == null || signals.isEmpty()) {
            return "unknown";
        }

        return signals.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("unknown");
    }

    private String humanizeSignal(String signal) {
        return switch (signal) {
            case "frictionDelta" -> "aumento de fricção funcional";
            case "ruptureDelta" -> "aumento de rupturas na trajetória";
            case "escalationDelta" -> "aumento de escalonamentos";
            case "continuityLossDelta" -> "perda de continuidade funcional";
            case "abandonDelta" -> "aumento de abandono";
            case "cleanCompletionDrop" -> "queda de conclusões limpas";
            case "averageFrictionScoreDelta" -> "aumento do esforço médio até conclusão";
            default -> "mudança comportamental relevante";
        };
    }
}
