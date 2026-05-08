package org.ohrly.core;

import org.junit.jupiter.api.Test;
import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.BehavioralPrimitiveType;
import org.ohrly.core.services.RuleBasedBehavioralConstructExtractorService;
import org.ohrly.core.valueObjects.BehavioralConstruct;
import org.ohrly.core.valueObjects.BehavioralPrimitive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RuleBasedBehavioralConstructExtractorTest {

    @Autowired
    private RuleBasedBehavioralConstructExtractorService extractor;

    @Test
    void shouldReturnEmptyListWhenPrimitivesAreEmpty() {
        var constructs = extractor.extract(List.of());

        assertThat(constructs).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenPrimitivesAreNull() {
        var constructs = extractor.extract(null);

        assertThat(constructs).isEmpty();
    }

    @Test
    void shouldDetectCleanCompletion() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START),
                primitive(BehavioralPrimitiveType.STEP_COMPLETED),
                primitive(BehavioralPrimitiveType.COMPLETE)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .containsExactly(BehavioralConstructType.CLEAN_COMPLETION);
    }

    @Test
    void shouldDetectFrictionWhenThereIsRetry() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START),
                primitive(BehavioralPrimitiveType.RETRY),
                primitive(BehavioralPrimitiveType.COMPLETE)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(BehavioralConstructType.FRICTION);

        assertThat(constructs)
                .noneMatch(c -> c.type() == BehavioralConstructType.CLEAN_COMPLETION);
    }

    @Test
    void shouldDetectRuptureWhenStepFailedAndSessionWasAbandoned() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START),
                primitive(BehavioralPrimitiveType.STEP_FAILED),
                primitive(BehavioralPrimitiveType.ABANDON)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(
                        BehavioralConstructType.FRICTION,
                        BehavioralConstructType.RUPTURE,
                        BehavioralConstructType.CONTINUITY_LOSS
                );
    }

    @Test
    void shouldDetectEscalationWhenThereIsHumanHandoff() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START),
                primitive(BehavioralPrimitiveType.HUMAN_HANDOFF),
                primitive(BehavioralPrimitiveType.COMPLETE)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(BehavioralConstructType.ESCALATION);
    }

    @Test
    void shouldDetectRecoveryWhenFailureRetryAndCompleteExist() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START),
                primitive(BehavioralPrimitiveType.STEP_FAILED),
                primitive(BehavioralPrimitiveType.RETRY),
                primitive(BehavioralPrimitiveType.COMPLETE)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(
                        BehavioralConstructType.FRICTION,
                        BehavioralConstructType.RECOVERY
                );
    }

    @Test
    void shouldDetectLoopingBehaviorWhenLoopExists() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START),
                primitive(BehavioralPrimitiveType.LOOP),
                primitive(BehavioralPrimitiveType.TIMEOUT)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(
                        BehavioralConstructType.FRICTION,
                        BehavioralConstructType.LOOPING_BEHAVIOR,
                        BehavioralConstructType.CONTINUITY_LOSS
                );
    }

    @Test
    void shouldDetectContinuityLossWhenFrictionAndEscalationExist() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START),
                primitive(BehavioralPrimitiveType.WAIT),
                primitive(BehavioralPrimitiveType.RETRY),
                primitive(BehavioralPrimitiveType.HUMAN_HANDOFF),
                primitive(BehavioralPrimitiveType.COMPLETE)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(
                        BehavioralConstructType.FRICTION,
                        BehavioralConstructType.ESCALATION,
                        BehavioralConstructType.CONTINUITY_LOSS
                );
    }

    @Test
    void shouldDetectBillRequestDegradationScenario() {
        var primitives = List.of(
                primitive(BehavioralPrimitiveType.START, "REQUEST_BILL"),
                primitive(BehavioralPrimitiveType.STEP_COMPLETED, "REQUEST_BILL"),
                primitive(BehavioralPrimitiveType.STEP_FAILED, "GENERATE_BILL"),
                primitive(BehavioralPrimitiveType.RETRY, "GENERATE_BILL"),
                primitive(BehavioralPrimitiveType.WAIT, "GENERATE_BILL"),
                primitive(BehavioralPrimitiveType.HUMAN_HANDOFF, "GENERATE_BILL"),
                primitive(BehavioralPrimitiveType.ABANDON, "HUMAN_SUPPORT")
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(
                        BehavioralConstructType.FRICTION,
                        BehavioralConstructType.RUPTURE,
                        BehavioralConstructType.ESCALATION,
                        BehavioralConstructType.CONTINUITY_LOSS
                );

        assertThat(constructs)
                .noneMatch(c -> c.type() == BehavioralConstructType.CLEAN_COMPLETION);
    }

    private BehavioralPrimitive primitive(BehavioralPrimitiveType type) {
        return primitive(type, "any_step");
    }

    private BehavioralPrimitive primitive(BehavioralPrimitiveType type, String step) {
        return new BehavioralPrimitive(
                type,
                "session-123",
                step,
                Instant.parse("2026-05-08T10:00:00Z")
        );
    }
}
