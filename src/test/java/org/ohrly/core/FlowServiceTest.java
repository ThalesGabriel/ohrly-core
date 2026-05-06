package org.ohrly.core;

import org.junit.jupiter.api.Test;
import org.ohrly.core.domain.*;
import org.ohrly.core.enums.*;
import org.ohrly.core.services.FlowService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class FlowServiceTest {

    private final FlowService evaluator = new FlowService();

    private final String flowId = "second-card-copy";
    private final String sessionId = "session-1";
    private final Instant startedAt = Instant.parse("2026-05-04T10:00:00Z");

    @Test
    void shouldEvaluateSessionAsConsistentWhenAllRequiredStepsArePresentAndSessionEndsSuccessfully() {
        var flow = secondCardCopyFlow();
        var session = createSession();

        session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
        session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
        session.addEvent(event("identify_card", startedAt.plusSeconds(10)));
        session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
        session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

        session.closeAsSuccess(startedAt.plusSeconds(25));

        var evaluation = evaluator.evaluateSession(flow, session);

        assertThat(evaluation.isConsistent()).isTrue();
        assertThat(evaluation.getEndReason()).isEqualTo(FlowSessionEndReason.SUCCESS);
        assertThat(evaluation.getFindings()).isEmpty();
    }

    @Test
    void shouldDetectMissingRequiredStep() {
        var flow = secondCardCopyFlow();
        var session = createSession();

        session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
        session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
        // identify_card faltando
        session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
        session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

        session.closeAsSuccess(startedAt.plusSeconds(25));

        var evaluation = evaluator.evaluateSession(flow, session);

        assertThat(evaluation.isConsistent()).isFalse();

        assertThat(evaluation.getFindings())
                .extracting(FlowFinding::type)
                .contains(FlowFindingType.MISSING_REQUIRED_STEP);

        assertThat(evaluation.getFindings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("identify_card");
                    assertThat(finding.severity()).isEqualTo(FindingSeverity.HIGH);
                });
    }

    @Test
    void shouldDetectMissingFinalStep() {
        var flow = secondCardCopyFlow();
        var session = createSession();

        session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
        session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
        session.addEvent(event("identify_card", startedAt.plusSeconds(10)));
        session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
        // deliver_response faltando

        session.closeAsFailure(startedAt.plusSeconds(25));

        var evaluation = evaluator.evaluateSession(flow, session);

        assertThat(evaluation.isConsistent()).isFalse();

        assertThat(evaluation.getFindings())
                .extracting(FlowFinding::type)
                .contains(FlowFindingType.MISSING_FINAL_STEP);

        assertThat(evaluation.getFindings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("deliver_response");
                    assertThat(finding.severity()).isEqualTo(FindingSeverity.HIGH);
                });
    }

    @Test
    void shouldDetectTimeoutSession() {
        var flow = secondCardCopyFlow();
        var session = createSession();

        session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
        session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));

        session.closeAsTimeout(startedAt.plusSeconds(300));

        var evaluation = evaluator.evaluateSession(flow, session);

        assertThat(evaluation.isConsistent()).isFalse();
        assertThat(evaluation.getEndReason()).isEqualTo(FlowSessionEndReason.TIMEOUT);

        assertThat(evaluation.getFindings())
                .extracting(FlowFinding::type)
                .contains(FlowFindingType.TIMEOUT);
    }

    @Test
    void shouldDetectHandoffSession() {
        var flow = secondCardCopyFlow();
        var session = createSession();

        session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
        session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
        session.addEvent(event("transfer_to_human", startedAt.plusSeconds(20)));

        session.closeAsHumanHandoff(startedAt.plusSeconds(21));

        var evaluation = evaluator.evaluateSession(flow, session);

        assertThat(evaluation.isConsistent()).isFalse();
        assertThat(evaluation.getEndReason()).isEqualTo(FlowSessionEndReason.HANDOFF);

        assertThat(evaluation.getFindings())
                .extracting(FlowFinding::type)
                .contains(FlowFindingType.HANDOFF);
    }

    @Test
    void shouldDetectLateEventsAfterSessionClosure() {
        var flow = secondCardCopyFlow();
        var session = createSession();

        session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
        session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
        session.addEvent(event("identify_card", startedAt.plusSeconds(10)));
        session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
        session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

        session.closeAsSuccess(startedAt.plusSeconds(25));

        session.addEvent(event("post_completion_callback", startedAt.plusSeconds(30)));

        var evaluation = evaluator.evaluateSession(flow, session);

        assertThat(evaluation.isConsistent()).isTrue();

        assertThat(evaluation.getFindings())
                .extracting(FlowFinding::type)
                .contains(FlowFindingType.LATE_EVENTS);

        assertThat(evaluation.getFindings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("1 late event");
                    assertThat(finding.severity()).isEqualTo(FindingSeverity.MEDIUM);
                });
    }

    @Test
    void shouldRejectEvaluationWhenSessionBelongsToAnotherFlow() {
        var flow = secondCardCopyFlow();
        var anotherFlowId = "another-flow";
        var session = FlowSession.builder().sessionId(sessionId).flowId(anotherFlowId).status(FlowSessionStatus.OPEN).startedAt(startedAt).build();


        session.closeAsSuccess(startedAt.plusSeconds(10));

        assertThatThrownBy(() -> evaluator.evaluateSession(flow, session))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void shouldRejectEvaluationOfOpenSession() {
        var flow = secondCardCopyFlow();
        var session = createSession();

        session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));

        assertThatThrownBy(() -> evaluator.evaluateSession(flow, session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("open session");
    }

    @Test
    void shouldUseStepImportanceToDefineFindingSeverity() {
        var flow = new FlowDefinition(
                flowId,
                "Second Card Copy",
                "Fluxo de segunda via de cartão",
                true,
                List.of(
                        step("intent_detected", 1, FlowStepImportance.LOW, true),
                        step("authenticate_user", 2, FlowStepImportance.MEDIUM, true),
                        step("identify_card", 3, FlowStepImportance.HIGH, true),
                        step("deliver_response", 4, FlowStepImportance.HIGH, true)
                )
        );

        var session = createSession();

        session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

        session.closeAsSuccess(startedAt.plusSeconds(25));

        // when
        var evaluation = evaluator.evaluateSession(flow, session);

        // then
        assertThat(evaluation.getFindings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("intent_detected");
                    assertThat(finding.severity()).isEqualTo(FindingSeverity.LOW);
                });

        assertThat(evaluation.getFindings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("authenticate_user");
                    assertThat(finding.severity()).isEqualTo(FindingSeverity.MEDIUM);
                });

        assertThat(evaluation.getFindings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("identify_card");
                    assertThat(finding.severity()).isEqualTo(FindingSeverity.HIGH);
                });

        assertThat(evaluation.isConsistent()).isFalse();
    }

    private FlowDefinition secondCardCopyFlow() {
        return new FlowDefinition(
                flowId,
                "Second Card Copy",
                "Fluxo de emissão de segunda via de cartão",
                true,
                List.of(
                        step("intent_detected", 1, FlowStepImportance.MEDIUM, true),
                        step("authenticate_user", 2, FlowStepImportance.HIGH, true),
                        step("identify_card", 3, FlowStepImportance.HIGH, true),
                        step("generate_second_copy", 4, FlowStepImportance.HIGH, true),
                        step("deliver_response", 5, FlowStepImportance.HIGH, true)
                )
        );
    }

    private FlowStepDefinition step(
            String name,
            int order,
            FlowStepImportance importance,
            boolean required
    ) {
        return new FlowStepDefinition(
                name,
                name,
                name,
                order,
                importance,
                required
        );
    }

    private FlowSession createSession() {
        return FlowSession.builder().sessionId(sessionId).flowId(flowId).status(FlowSessionStatus.OPEN).startedAt(startedAt).build();
    }

    private FlowEvent event(String stepName, Instant timestamp) {
        return new FlowEvent(
                UUID.randomUUID().toString(),
                flowId,
                sessionId,
                timestamp,
                stepName,
                "success",
                "v1",
                Map.of()
        );
    }
}
