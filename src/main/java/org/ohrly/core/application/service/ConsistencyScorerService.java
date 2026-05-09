package org.ohrly.core.application.service;

import org.ohrly.core.application.domain.FlowConsistencyScore;
import org.ohrly.core.application.domain.FlowEvaluation;
import org.ohrly.core.application.domain.FlowFinding;
import org.ohrly.core.application.type.FlowConsistencyLevelType;
import org.ohrly.core.application.type.FlowFindingSeverityType;
import org.springframework.stereotype.Service;

import java.util.Objects;

// TODO: Externalizar pesos de penalidade para uma policy/config por tipo de fluxo.
// TODO: Permitir severidade dinâmica por FlowFindingType + criticidade da etapa.
// TODO: Evitar dupla penalização excessiva quando findings têm a mesma causa raiz.
// TODO: Adicionar limite máximo de penalidade por categoria de finding.
// TODO: Registrar breakdown do score para explicar por que a sessão recebeu determinada nota.
// TODO: Criar objeto FlowConsistencyScoreBreakdown com penalties aplicadas.
// TODO: Permitir diferentes estratégias de scoring por fluxo, canal ou criticidade.
// TODO: Adicionar confidence score quando houver poucos eventos, sessão incompleta ou dados inconsistentes.
// TODO: Tratar findings desconhecidos ou futuros sem quebrar o cálculo.
// TODO: Cobrir casos de borda: findings nulos, lista vazia, penalidade acima de 100 e severidade nula.
// TODO: Tornar o score explicável, retornando não apenas o valor final,mas também quais findings causaram quais penalidades.

@Service
public class ConsistencyScorerService {

    private static final int MAX_SCORE = 100;
    private static final int MIN_SCORE = 0;

    public FlowConsistencyScore score(FlowEvaluation evaluation) {
        Objects.requireNonNull(evaluation, "evaluation cannot be null");

        int penalty = evaluation.getFindings().stream()
                .mapToInt(this::penaltyFor)
                .sum();

        int finalScore = Math.max(MIN_SCORE, MAX_SCORE - penalty);

        return new FlowConsistencyScore(
                evaluation.getFlowId(),
                evaluation.getSessionId(),
                finalScore,
                consistencyLevel(finalScore)
        );
    }

    private int penaltyFor(FlowFinding finding) {
        return switch (finding.getType()) {
            case MISSING_FINAL_STEP -> 40;
            case TIMEOUT -> 35;
            case HANDOFF -> 30;
            case MISSING_REQUIRED_STEP -> penaltyBySeverity(finding.getSeverity());
            case LATE_EVENTS -> 10;
        };
    }

    private int penaltyBySeverity(FlowFindingSeverityType severity) {
        return switch (severity) {
            case HIGH -> 30;
            case MEDIUM -> 15;
            case LOW -> 5;
        };
    }

    private FlowConsistencyLevelType consistencyLevel(int score) {
        if (score >= 85) {
            return FlowConsistencyLevelType.HEALTHY;
        }

        if (score >= 70) {
            return FlowConsistencyLevelType.ATTENTION;
        }

        return FlowConsistencyLevelType.DEGRADED;
    }
}
