package org.ohrly.core;

import org.junit.jupiter.api.Test;
import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.BehavioralPrimitiveType;
import org.ohrly.core.services.TrajectoryAwareBehavioralConstructExtractorService;
import org.ohrly.core.valueObjects.BehavioralConstruct;
import org.ohrly.core.valueObjects.BehavioralPrimitive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TrajectoryAwareBehavioralConstructExtractorServiceTest {

    @Autowired
    private TrajectoryAwareBehavioralConstructExtractorService extractor;

    @Test
    void shouldDetectRecoveryWhenFailureHappensBeforeCompletion() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.STEP_FAILED),
                primitive(2, BehavioralPrimitiveType.RETRY),
                primitive(3, BehavioralPrimitiveType.COMPLETE)
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
    void shouldNotDetectRecoveryWhenFailureHappensAfterCompletion() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.COMPLETE),
                primitive(2, BehavioralPrimitiveType.STEP_FAILED),
                primitive(3, BehavioralPrimitiveType.RETRY)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .doesNotContain(BehavioralConstructType.RECOVERY);
    }

    @Test
    void shouldDetectRuptureWhenFailurePrecedesAbandon() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.STEP_FAILED),
                primitive(2, BehavioralPrimitiveType.ABANDON)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(
                        BehavioralConstructType.RUPTURE,
                        BehavioralConstructType.CONTINUITY_LOSS
                );
    }

    @Test
    void shouldNotDetectRuptureWhenAbandonPrecedesFailure() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.ABANDON),
                primitive(2, BehavioralPrimitiveType.STEP_FAILED)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .doesNotContain(BehavioralConstructType.RUPTURE);
    }

    @Test
    void shouldDetectContinuityLossWhenFrictionPrecedesEscalation() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.RETRY),
                primitive(2, BehavioralPrimitiveType.WAIT),
                primitive(3, BehavioralPrimitiveType.HUMAN_HANDOFF),
                primitive(4, BehavioralPrimitiveType.COMPLETE)
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
    void shouldNotDetectContinuityLossWhenEscalationPrecedesFriction() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.HUMAN_HANDOFF),
                primitive(2, BehavioralPrimitiveType.RETRY),
                primitive(3, BehavioralPrimitiveType.COMPLETE)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .doesNotContain(BehavioralConstructType.CONTINUITY_LOSS);
    }

    @Test
    void shouldDetectLoopingBehaviorWhenMultipleRetriesExist() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.RETRY),
                primitive(2, BehavioralPrimitiveType.RETRY),
                primitive(3, BehavioralPrimitiveType.RETRY),
                primitive(4, BehavioralPrimitiveType.TIMEOUT)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .contains(BehavioralConstructType.LOOPING_BEHAVIOR);
    }

    @Test
    void shouldDetectCleanCompletionWhenNoFrictionOrEscalationExists() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.STEP_COMPLETED),
                primitive(2, BehavioralPrimitiveType.COMPLETE)
        );

        var constructs = extractor.extract(primitives);

        assertThat(constructs)
                .extracting(BehavioralConstruct::type)
                .containsExactly(BehavioralConstructType.CLEAN_COMPLETION);
    }

    @Test
    void shouldDetectBillRequestBehavioralDegradationTrajectory() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.STEP_COMPLETED),
                primitive(2, BehavioralPrimitiveType.STEP_FAILED),
                primitive(3, BehavioralPrimitiveType.RETRY),
                primitive(4, BehavioralPrimitiveType.WAIT),
                primitive(5, BehavioralPrimitiveType.HUMAN_HANDOFF),
                primitive(6, BehavioralPrimitiveType.ABANDON)
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
                .extracting(BehavioralConstruct::type)
                .doesNotContain(
                        BehavioralConstructType.CLEAN_COMPLETION,
                        BehavioralConstructType.RECOVERY
                );
    }

    private BehavioralPrimitive primitive(
            int minuteOffset,
            BehavioralPrimitiveType type
    ) {
        return new BehavioralPrimitive(
                type,
                "session-123",
                "GENERATE_BILL",
                Instant.parse("2026-05-08T10:00:00Z")
                        .plusSeconds(minuteOffset * 60L)
        );
    }
}
