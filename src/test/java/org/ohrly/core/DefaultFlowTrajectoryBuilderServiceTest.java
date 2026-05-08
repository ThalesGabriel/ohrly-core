package org.ohrly.core;

import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.BehavioralPrimitiveType;
import org.ohrly.core.enums.TrajectoryOutcomeType;
import org.ohrly.core.services.DefaultFlowTrajectoryBuilderService;
import org.ohrly.core.services.TrajectoryAwareBehavioralConstructExtractorService;
import org.ohrly.core.valueObjects.BehavioralConstruct;
import org.ohrly.core.valueObjects.BehavioralPrimitive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DefaultFlowTrajectoryBuilderServiceTest {

    @Autowired
    private DefaultFlowTrajectoryBuilderService builder;

    @Test
    void shouldThrowExceptionWhenPrimitivesAreEmpty() {
        assertThatThrownBy(() ->
                builder.build("bill_request", "session-123", List.of())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("primitives must not be empty");
    }

    @Test
    void shouldThrowExceptionWhenPrimitivesAreNull() {
        assertThatThrownBy(() ->
                builder.build("bill_request", "session-123", null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("primitives must not be empty");
    }

    @Test
    void shouldBuildCompletedTrajectory() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.STEP_COMPLETED),
                primitive(2, BehavioralPrimitiveType.COMPLETE)
        );

        var trajectory = builder.build(
                "bill_request",
                "session-123",
                primitives
        );

        assertThat(trajectory.sessionId()).isEqualTo("session-123");
        assertThat(trajectory.flowId()).isEqualTo("bill_request");
        assertThat(trajectory.outcome()).isEqualTo(TrajectoryOutcomeType.COMPLETED);
        assertThat(trajectory.isFinished()).isTrue();
        assertThat(trajectory.completedCleanly()).isTrue();
        assertThat(trajectory.hasFriction()).isFalse();
        assertThat(trajectory.hasRupture()).isFalse();
        assertThat(trajectory.hasEscalation()).isFalse();
    }

    @Test
    void shouldBuildAbandonedTrajectory() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.STEP_FAILED),
                primitive(2, BehavioralPrimitiveType.ABANDON)
        );

        var trajectory = builder.build(
                "bill_request",
                "session-123",
                primitives
        );

        assertThat(trajectory.outcome()).isEqualTo(TrajectoryOutcomeType.ABANDONED);
        assertThat(trajectory.isFinished()).isTrue();
        assertThat(trajectory.hasFriction()).isTrue();
        assertThat(trajectory.hasRupture()).isTrue();
        assertThat(trajectory.hasContinuityLoss()).isTrue();
    }

    @Test
    void shouldBuildTimedOutTrajectory() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.LOOP),
                primitive(2, BehavioralPrimitiveType.TIMEOUT)
        );

        var trajectory = builder.build(
                "bill_request",
                "session-123",
                primitives
        );

        assertThat(trajectory.outcome()).isEqualTo(TrajectoryOutcomeType.TIMED_OUT);
        assertThat(trajectory.isFinished()).isTrue();
        assertThat(trajectory.hasFriction()).isTrue();
        assertThat(trajectory.hasContinuityLoss()).isTrue();
    }

    @Test
    void shouldBuildTransferredTrajectory() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.HUMAN_HANDOFF)
        );

        var trajectory = builder.build(
                "bill_request",
                "session-123",
                primitives
        );

        assertThat(trajectory.outcome()).isEqualTo(TrajectoryOutcomeType.TRANSFERRED);
        assertThat(trajectory.isFinished()).isTrue();
        assertThat(trajectory.hasEscalation()).isTrue();
    }

    @Test
    void shouldBuildOpenTrajectoryWhenThereIsNoTerminalPrimitive() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START),
                primitive(1, BehavioralPrimitiveType.STEP_REACHED)
        );

        var trajectory = builder.build(
                "bill_request",
                "session-123",
                primitives
        );

        assertThat(trajectory.outcome()).isEqualTo(TrajectoryOutcomeType.OPEN);
        assertThat(trajectory.isFinished()).isFalse();
        assertThat(trajectory.finishedAt()).isNull();
    }

    @Test
    void shouldOrderPrimitivesByTimestamp() {
        var primitives = List.of(
                primitive(3, BehavioralPrimitiveType.COMPLETE),
                primitive(0, BehavioralPrimitiveType.START),
                primitive(2, BehavioralPrimitiveType.STEP_COMPLETED)
        );

        var trajectory = builder.build(
                "bill_request",
                "session-123",
                primitives
        );

        assertThat(trajectory.primitives())
                .extracting(BehavioralPrimitive::type)
                .containsExactly(
                        BehavioralPrimitiveType.START,
                        BehavioralPrimitiveType.STEP_COMPLETED,
                        BehavioralPrimitiveType.COMPLETE
                );

        assertThat(trajectory.startedAt())
                .isEqualTo(Instant.parse("2026-05-08T10:00:00Z"));
    }

    @Test
    void shouldBuildBillRequestDegradationTrajectory() {
        var primitives = List.of(
                primitive(0, BehavioralPrimitiveType.START, "START"),
                primitive(1, BehavioralPrimitiveType.STEP_COMPLETED, "REQUEST_BILL"),
                primitive(2, BehavioralPrimitiveType.STEP_FAILED, "GENERATE_BILL"),
                primitive(3, BehavioralPrimitiveType.RETRY, "GENERATE_BILL"),
                primitive(4, BehavioralPrimitiveType.WAIT, "GENERATE_BILL"),
                primitive(5, BehavioralPrimitiveType.HUMAN_HANDOFF, "GENERATE_BILL"),
                primitive(6, BehavioralPrimitiveType.ABANDON, "HUMAN_SUPPORT")
        );

        var trajectory = builder.build(
                "bill_request",
                "session-123",
                primitives
        );

        assertThat(trajectory.outcome()).isEqualTo(TrajectoryOutcomeType.ABANDONED);

        assertThat(trajectory.constructs())
                .extracting(BehavioralConstruct::type)
                .contains(
                        BehavioralConstructType.FRICTION,
                        BehavioralConstructType.RUPTURE,
                        BehavioralConstructType.ESCALATION,
                        BehavioralConstructType.CONTINUITY_LOSS
                );

        assertThat(trajectory.completedCleanly()).isFalse();
        assertThat(trajectory.hasFriction()).isTrue();
        assertThat(trajectory.hasRupture()).isTrue();
        assertThat(trajectory.hasEscalation()).isTrue();
        assertThat(trajectory.hasContinuityLoss()).isTrue();
        assertThat(trajectory.hasRecovery()).isFalse();
    }

    private BehavioralPrimitive primitive(
            int minuteOffset,
            BehavioralPrimitiveType type
    ) {
        return primitive(minuteOffset, type, "any_step");
    }

    private BehavioralPrimitive primitive(
            int minuteOffset,
            BehavioralPrimitiveType type,
            String step
    ) {
        return new BehavioralPrimitive(
                type,
                "session-123",
                step,
                Instant.parse("2026-05-08T10:00:00Z")
                        .plusSeconds(minuteOffset * 60L)
        );
    }
}
