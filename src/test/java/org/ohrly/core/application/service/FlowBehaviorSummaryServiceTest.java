package org.ohrly.core.application.service;

import org.junit.jupiter.api.Test;
import org.ohrly.core.application.service.FlowBehaviorSummaryService;
import org.ohrly.core.application.type.BehavioralConstructType;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.application.type.FlowOutcomeType;
import org.ohrly.core.application.valueObject.BehavioralConstruct;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.ohrly.core.application.valueObject.FlowBehaviorSummary;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlowBehaviorSummaryServiceTest {

    @Autowired
    private FlowBehaviorSummaryService service;

    @Test
    void shouldSummarizeBehaviorFromTrajectories() {
        List<FlowTrajectory> trajectories = List.of(
                trajectory(
                        FlowOutcomeType.COMPLETED,
                        construct(BehavioralConstructType.CLEAN_COMPLETION, 1.0)
                ),
                trajectory(
                        FlowOutcomeType.COMPLETED,
                        construct(BehavioralConstructType.FRICTION, 0.7),
                        construct(BehavioralConstructType.RECOVERY, 0.8)
                ),
                trajectory(
                        FlowOutcomeType.TRANSFERRED,
                        construct(BehavioralConstructType.ESCALATION, 1.0)
                ),
                trajectory(
                        FlowOutcomeType.ABANDONED,
                        construct(BehavioralConstructType.RUPTURE, 0.9),
                        construct(BehavioralConstructType.CONTINUITY_LOSS, 1.0)
                ),
                trajectory(
                        FlowOutcomeType.TIMED_OUT,
                        construct(BehavioralConstructType.FRICTION, 0.5)
                )
        );

        FlowBehaviorSummary summary =
                service.summarize(trajectories);

        assertThat(summary.totalSessions())
                .isEqualTo(5);

        assertThat(summary.cleanCompletionRate())
                .isEqualTo(0.2);

        assertThat(summary.frictionRate())
                .isEqualTo(0.4);

        assertThat(summary.ruptureRate())
                .isEqualTo(0.2);

        assertThat(summary.escalationRate())
                .isEqualTo(0.2);

        assertThat(summary.recoveryRate())
                .isEqualTo(0.2);

        assertThat(summary.continuityLossRate())
                .isEqualTo(0.2);

        assertThat(summary.abandonRate())
                .isEqualTo(0.2);

        assertThat(summary.timeoutRate())
                .isEqualTo(0.2);

        assertThat(summary.averageFrictionScore())
                .isEqualTo(0.6);

        assertThat(summary.constructCounts())
                .containsEntry(BehavioralConstructType.FRICTION, 2L)
                .containsEntry(BehavioralConstructType.CLEAN_COMPLETION, 1L)
                .containsEntry(BehavioralConstructType.RECOVERY, 1L)
                .containsEntry(BehavioralConstructType.ESCALATION, 1L)
                .containsEntry(BehavioralConstructType.RUPTURE, 1L)
                .containsEntry(BehavioralConstructType.CONTINUITY_LOSS, 1L);
    }

    @Test
    void shouldReturnEmptySummaryWhenThereAreNoTrajectories() {
        FlowBehaviorSummary summary =
                service.summarize(List.of());

        assertThat(summary.totalSessions())
                .isZero();

        assertThat(summary.cleanCompletionRate())
                .isZero();

        assertThat(summary.frictionRate())
                .isZero();

        assertThat(summary.ruptureRate())
                .isZero();

        assertThat(summary.escalationRate())
                .isZero();

        assertThat(summary.recoveryRate())
                .isZero();

        assertThat(summary.continuityLossRate())
                .isZero();

        assertThat(summary.abandonRate())
                .isZero();

        assertThat(summary.timeoutRate())
                .isZero();

        assertThat(summary.averageFrictionScore())
                .isZero();

        assertThat(summary.constructCounts())
                .isEmpty();
    }

    private FlowTrajectory trajectory(
            FlowOutcomeType outcome,
            BehavioralConstruct... constructs
    ) {
        String sessionId = "session-" + outcome.name().toLowerCase();

        return new FlowTrajectory(
                sessionId,
                "flow-1",
                Instant.parse("2026-05-08T10:00:00Z"),
                Instant.parse("2026-05-08T10:05:00Z"),
                outcome,
                List.of(
                        BehavioralPrimitive.of(
                                BehavioralPrimitiveType.START,
                                sessionId,
                                Instant.parse("2026-05-08T10:00:00Z")
                        )
                ),
                List.of(constructs),
                Map.of("channel", "whatsapp")
        );
    }

    private BehavioralConstruct construct(
            BehavioralConstructType type,
            double score
    ) {
        return new BehavioralConstruct(
                type,
                List.of(),
                score,
                "test construct"
        );
    }
}