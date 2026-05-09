package org.ohrly.core.application.service;

import org.junit.jupiter.api.Test;
import org.ohrly.core.application.factory.FlowTrajectoryFactory;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.application.type.FlowOutcomeType;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlowTrajectoryBuilderServiceTest {

    @Autowired
    private FlowTrajectoryFactory builder;

    @Test
    void shouldBuildCompletedTrajectory() {

        List<BehavioralPrimitive> primitives = List.of(
                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.START,
                        "session-1",
                        Instant.now()
                ),

                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.STEP_COMPLETED,
                        "session-1",
                        "identify_customer",
                        Instant.now()
                ),

                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.COMPLETE,
                        "session-1",
                        Instant.now()
                )
        );

        FlowTrajectory trajectory = builder.create(
                "bill_copy",
                "session-1",
                primitives
        );

        assertThat(trajectory).isNotNull();

        assertThat(trajectory.flowId())
                .isEqualTo("bill_copy");

        assertThat(trajectory.sessionId())
                .isEqualTo("session-1");

        assertThat(trajectory.outcome())
                .isEqualTo(FlowOutcomeType.COMPLETED);

        assertThat(trajectory.primitives())
                .hasSize(3);
    }

    @Test
    void shouldBuildTimedOutTrajectory() {

        List<BehavioralPrimitive> primitives = List.of(
                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.START,
                        "session-2",
                        Instant.now()
                ),

                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.TIMEOUT,
                        "session-2",
                        Instant.now()
                )
        );

        FlowTrajectory trajectory = builder.create(
                "bill_copy",
                "session-2",
                primitives
        );

        assertThat(trajectory.outcome())
                .isEqualTo(FlowOutcomeType.TIMED_OUT);
    }

    @Test
    void shouldBuildTransferredTrajectory() {

        List<BehavioralPrimitive> primitives = List.of(
                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.START,
                        "session-3",
                        Instant.now()
                ),

                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.HUMAN_HANDOFF,
                        "session-3",
                        Instant.now()
                )
        );

        FlowTrajectory trajectory = builder.create(
                "bill_copy",
                "session-3",
                primitives
        );

        assertThat(trajectory.outcome())
                .isEqualTo(FlowOutcomeType.TRANSFERRED);
    }

    @Test
    void shouldBuildOpenTrajectoryWhenNoFinalPrimitiveExists() {

        List<BehavioralPrimitive> primitives = List.of(
                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.START,
                        "session-4",
                        Instant.now()
                ),

                BehavioralPrimitive.of(
                        BehavioralPrimitiveType.STEP_COMPLETED,
                        "session-4",
                        "generate_bill",
                        Instant.now()
                )
        );

        FlowTrajectory trajectory = builder.create(
                "bill_copy",
                "session-4",
                primitives
        );

        assertThat(trajectory.outcome())
                .isEqualTo(FlowOutcomeType.OPEN);
    }

}