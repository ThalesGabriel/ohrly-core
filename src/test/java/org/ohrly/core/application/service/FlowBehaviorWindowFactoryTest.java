package org.ohrly.core.application.service;

import org.junit.jupiter.api.Test;
import org.ohrly.core.application.factory.FlowBehaviorWindowFactory;
import org.ohrly.core.application.type.BehavioralConstructType;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.application.type.FlowOutcomeType;
import org.ohrly.core.application.valueObject.BehavioralConstruct;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlowBehaviorWindowFactoryTest {

    @Autowired
    private FlowBehaviorWindowFactory builder;

    @Test
    void shouldBuildBehaviorWindowSummary() {
        var clean = trajectory(
                "s1",
                FlowOutcomeType.COMPLETED,
                List.of(new BehavioralConstruct(
                        BehavioralConstructType.CLEAN_COMPLETION,
                        List.of(
                                BehavioralPrimitiveType.START,
                                BehavioralPrimitiveType.COMPLETE
                        ),
                        1.0,
                        "Clean completion"
                ))
        );

        var degraded = trajectory(
                "s2",
                FlowOutcomeType.ABANDONED,
                List.of(
                        new BehavioralConstruct(
                                BehavioralConstructType.FRICTION,
                                List.of(
                                        BehavioralPrimitiveType.STEP_FAILED,
                                        BehavioralPrimitiveType.RETRY
                                ),
                                7.0,
                                "Friction detected"
                        ),
                        new BehavioralConstruct(
                                BehavioralConstructType.RUPTURE,
                                List.of(
                                        BehavioralPrimitiveType.STEP_FAILED,
                                        BehavioralPrimitiveType.ABANDON
                                ),
                                9.0,
                                "Rupture detected"
                        ),
                        new BehavioralConstruct(
                                BehavioralConstructType.CONTINUITY_LOSS,
                                List.of(
                                        BehavioralPrimitiveType.RETRY,
                                        BehavioralPrimitiveType.ABANDON
                                ),
                                8.5,
                                "Continuity loss detected"
                        )
                )
        );

        var window = builder.build(
                "bill_request",
                Instant.parse("2026-05-08T00:00:00Z"),
                Instant.parse("2026-05-09T00:00:00Z"),
                List.of(clean, degraded)
        );

        assertThat(window.flowId()).isEqualTo("bill_request");
        assertThat(window.totalSessions()).isEqualTo(2);

        assertThat(window.summary().totalSessions()).isEqualTo(2);
        assertThat(window.summary().cleanCompletionRate()).isEqualTo(0.5);
        assertThat(window.summary().frictionRate()).isEqualTo(0.5);
        assertThat(window.summary().ruptureRate()).isEqualTo(0.5);
        assertThat(window.summary().continuityLossRate()).isEqualTo(0.5);
        assertThat(window.summary().abandonRate()).isEqualTo(0.5);
        assertThat(window.summary().averageFrictionScore()).isEqualTo(7.0);

        assertThat(window.summary().constructCounts())
                .containsEntry(BehavioralConstructType.FRICTION, 1L)
                .containsEntry(BehavioralConstructType.RUPTURE, 1L)
                .containsEntry(BehavioralConstructType.CONTINUITY_LOSS, 1L)
                .containsEntry(BehavioralConstructType.CLEAN_COMPLETION, 1L);
    }

    private FlowTrajectory trajectory(
            String sessionId,
            FlowOutcomeType outcome,
            List<BehavioralConstruct> constructs
    ) {
        return new FlowTrajectory(
                sessionId,
                "bill_request",
                Instant.parse("2026-05-08T10:00:00Z"),
                Instant.parse("2026-05-08T10:05:00Z"),
                outcome,
                List.of(),
                constructs,
                Map.of("channel", "whatsapp")
        );
    }
}
