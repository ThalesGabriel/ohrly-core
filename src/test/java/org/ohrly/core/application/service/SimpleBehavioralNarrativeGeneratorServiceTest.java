package org.ohrly.core.application.service;

import org.junit.jupiter.api.Test;
import org.ohrly.core.application.service.SimpleBehavioralNarrativeGeneratorService;
import org.ohrly.core.application.type.BehavioralDriftStateType;
import org.ohrly.core.application.valueObject.BehavioralDriftResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SimpleBehavioralNarrativeGeneratorServiceTest {

    @Autowired
    private SimpleBehavioralNarrativeGeneratorService generator;

    @Test
    void shouldGenerateNormalNarrative() {
        var result = result(
                BehavioralDriftStateType.NORMAL,
                Map.of("frictionDelta", 0.02)
        );

        var narrative = generator.generate(result);

        assertThat(narrative)
                .contains("permanece dentro do comportamento esperado")
                .contains("Não há sinais relevantes");
    }

    @Test
    void shouldGenerateAttentionNarrative() {
        var result = result(
                BehavioralDriftStateType.ATTENTION,
                Map.of("frictionDelta", 0.12)
        );

        var narrative = generator.generate(result);

        assertThat(narrative)
                .contains("começou a apresentar sinais iniciais")
                .contains("aumento de fricção funcional");
    }

    @Test
    void shouldGenerateDegradedNarrative() {
        var result = result(
                BehavioralDriftStateType.DEGRADED,
                Map.of(
                        "frictionDelta", 0.18,
                        "continuityLossDelta", 0.28
                )
        );

        var narrative = generator.generate(result);

        assertThat(narrative)
                .contains("entrou em degradação funcional")
                .contains("perda de continuidade funcional")
                .contains("risco de impacto operacional progressivo");
    }

    @Test
    void shouldGenerateCriticalNarrative() {
        var result = result(
                BehavioralDriftStateType.CRITICAL,
                Map.of(
                        "abandonDelta", 0.41,
                        "frictionDelta", 0.21
                )
        );

        var narrative = generator.generate(result);

        assertThat(narrative)
                .contains("degradação funcional crítica")
                .contains("aumento de abandono")
                .contains("impacto operacional imediato");
    }

    @Test
    void shouldRejectNullResult() {
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("result must not be null");
    }

    private BehavioralDriftResult result(
            BehavioralDriftStateType state,
            Map<String, Double> signals
    ) {
        return new BehavioralDriftResult(
                "bill_request",
                state,
                signals.getOrDefault("frictionDelta", 0.0),
                signals.getOrDefault("ruptureDelta", 0.0),
                signals.getOrDefault("escalationDelta", 0.0),
                signals.getOrDefault("continuityLossDelta", 0.0),
                signals.getOrDefault("abandonDelta", 0.0),
                signals.getOrDefault("cleanCompletionDrop", 0.0),
                signals.getOrDefault("averageFrictionScoreDelta", 0.0),
                signals
        );
    }
}
