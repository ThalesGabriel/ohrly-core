package org.ohrly.core;

import org.junit.jupiter.api.Test;
import org.ohrly.core.domain.FlowEvent;
import org.ohrly.core.domain.FlowSession;
import org.ohrly.core.enums.FlowSessionEndReason;
import org.ohrly.core.enums.FlowSessionStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class FlowSessionTest {

    private final String sessionId = "session-1";
    private final String flowId = "flow-1";
    private final Instant startedAt = Instant.now();

    @Test
    void shouldCreateOpenSession() {
         var session = createOpenSession();

        assertThat(session.getSessionId()).isEqualTo(sessionId);
        assertThat(session.getFlowId()).isEqualTo(flowId);
        assertThat(session.getStartedAt()).isEqualTo(startedAt);
        assertThat(session.getStatus()).isEqualTo(FlowSessionStatus.OPEN);
        assertThat(session.isOpen()).isTrue();
        assertThat(session.isClosed()).isFalse();
        assertThat(session.getEvents()).isEmpty();
    }

    @Test
    void shouldAddEventToOpenSession() {
        var session = createOpenSession();
        var event = event("authenticate_user", "success", startedAt.plusSeconds(10));

        session.addEvent(event);

        assertThat(session.getEvents()).containsExactly(event);
        assertThat(session.lastEvent().get().getOccurredAt()).isEqualTo(event.getOccurredAt());
    }

    @Test
    void shouldRejectEventFromAnotherSession() {
        var session = createOpenSession();

        var event = FlowEvent.builder().sessionId("another-session").flowId(flowId).build();

        assertThatThrownBy(() -> session.addEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("another session");
    }

    @Test
    void shouldRejectEventFromAnotherFlow() {
        var session = createOpenSession();

        var event = FlowEvent.builder().sessionId(sessionId).flowId("another-flow").build();

        assertThatThrownBy(() -> session.addEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("another flow");
    }

    @Test
    void shouldOrderEventsByTimestamp() {
        var session = createOpenSession();

        var third = event("generate_second_copy", "success", startedAt.plusSeconds(30));
        var first = event("intent_detected", "success", startedAt.plusSeconds(5));
        var second = event("authenticate_user", "success", startedAt.plusSeconds(15));

        session.addEvent(third);
        session.addEvent(first);
        session.addEvent(second);

        assertThat(session.orderedEvents())
                .extracting(FlowEvent::getStepName)
                .containsExactly(
                        "intent_detected",
                        "authenticate_user",
                        "generate_second_copy"
                );
    }

    @Test
    void shouldReturnLastEventBasedOnTimestamp() {
        var session = createOpenSession();

        session.addEvent(event("intent_detected", "success", startedAt.plusSeconds(5)));
        session.addEvent(event("authenticate_user", "success", startedAt.plusSeconds(15)));
        session.addEvent(event("generate_second_copy", "success", startedAt.plusSeconds(30)));

        assertThat(session.lastEvent())
                .isPresent()
                .get()
                .extracting(FlowEvent::getStepName)
                .isEqualTo("generate_second_copy");
    }

    @Test
    void shouldDetectContainedStepIgnoringCase() {
        var session = createOpenSession();

        session.addEvent(event("Authenticate_User", "success", startedAt.plusSeconds(10)));

        assertThat(session.containsStep("authenticate_user")).isTrue();
        assertThat(session.containsStep("generate_second_copy")).isFalse();
    }

    @Test
    void shouldCloseSessionAsSuccess() {
        var session = createOpenSession();
        var endedAt = startedAt.plusSeconds(60);

        session.closeAsSuccess(endedAt);

        assertThat(session.isClosed()).isTrue();
        assertThat(session.getStatus()).isEqualTo(FlowSessionStatus.CLOSED);
        assertThat(session.getEndReason()).isEqualTo(FlowSessionEndReason.SUCCESS);
        assertThat(session.getEndedAt()).isEqualTo(endedAt);
    }

    @Test
    void shouldCloseSessionAsFailure() {
        var session = createOpenSession();
        var endedAt = startedAt.plusSeconds(60);

        session.closeAsFailure(endedAt);

        assertThat(session.isClosed()).isTrue();
        assertThat(session.getEndReason()).isEqualTo(FlowSessionEndReason.FAILURE);
    }

    @Test
    void shouldCloseSessionAsHumanHandoff() {
        var session = createOpenSession();
        var endedAt = startedAt.plusSeconds(60);

        session.closeAsHumanHandoff(endedAt);

        assertThat(session.isClosed()).isTrue();
        assertThat(session.getEndReason()).isEqualTo(FlowSessionEndReason.HANDOFF);
    }

    @Test
    void shouldCloseSessionAsTimeout() {
        var session = createOpenSession();
        var endedAt = startedAt.plusSeconds(300);

        session.closeAsTimeout(endedAt);

        assertThat(session.isClosed()).isTrue();
        assertThat(session.getEndReason()).isEqualTo(FlowSessionEndReason.TIMEOUT);
    }

    @Test
    void shouldRegisterLateEventWhenEventArrivesAfterSessionIsClosed() {
        // given
        var session = createOpenSession();

        var firstEvent = event("intent_detected", "success", startedAt.plusSeconds(5));
        var secondEvent = event("authenticate_user", "success", startedAt.plusSeconds(10));

        session.addEvent(firstEvent);
        session.addEvent(secondEvent);

        var endedAt = startedAt.plusSeconds(20);
        session.closeAsSuccess(endedAt);

        var lateEvent = event("post_completion_callback", "success", startedAt.plusSeconds(30));

        session.addEvent(lateEvent);

        assertThat(session.getEvents())
                .containsExactly(firstEvent, secondEvent);

        assertThat(session.getLateEvents())
                .containsExactly(lateEvent);

        assertThat(session.isClosed()).isTrue();
        assertThat(session.getEndReason()).isEqualTo(FlowSessionEndReason.SUCCESS);
        assertThat(session.getEndedAt()).isEqualTo(endedAt);

        assertThat(session.lastEvent().get().getOccurredAt()).isEqualTo(secondEvent.getOccurredAt());
    }

    @Test
    void shouldNotAllowClosingSessionTwice() {
        var session = createOpenSession();

        session.closeAsFailure(startedAt.plusSeconds(60));

        assertThatThrownBy(() -> session.closeAsTimeout(startedAt.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void shouldDetectExceededTimeoutForOpenSession() {
        var session = createOpenSession();

        session.addEvent(event("intent_detected", "success", startedAt.plusSeconds(10)));

        var timeout = Duration.ofMinutes(5);
        var now = startedAt.plus(6, ChronoUnit.MINUTES);

        assertThat(session.exceededTimeout(timeout, now)).isTrue();
    }

    @Test
    void shouldNotDetectTimeoutBeforeLimit() {
        var session = createOpenSession();

        session.addEvent(event("intent_detected", "success", startedAt.plusSeconds(10)));

        var timeout = Duration.ofMinutes(5);
        var now = startedAt.plus(4, ChronoUnit.MINUTES);

        assertThat(session.exceededTimeout(timeout, now)).isFalse();
    }

    @Test
    void shouldNotDetectTimeoutForClosedSession() {
        var session = createOpenSession();

        session.closeAsSuccess(startedAt.plusSeconds(60));

        assertThat(session.exceededTimeout(Duration.ofMinutes(5), startedAt.plus(10, ChronoUnit.MINUTES)))
                .isFalse();
    }

    @Test
    void shouldCalculateDurationUntilNowForOpenSession() {
        var session = createOpenSession();

        var duration = session.durationUntilNow(startedAt.plusSeconds(90));

        assertThat(duration).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    void shouldCalculateFinalDurationForClosedSession() {
        var session = createOpenSession();

        session.closeAsSuccess(startedAt.plusSeconds(120));

        var duration = session.durationUntilNow(startedAt.plusSeconds(300));

        assertThat(duration).isEqualTo(Duration.ofSeconds(120));
    }

    private FlowSession createOpenSession() {
        return FlowSession.builder().sessionId(sessionId).flowId(flowId).status(FlowSessionStatus.OPEN).startedAt(startedAt).build();
    }

    private FlowEvent event(String stepName, String status, Instant timestamp) {
        return FlowEvent.builder()
                .sessionId(sessionId)
                .flowId(flowId)
                .stepName(stepName)
                .status(status)
                .occurredAt(timestamp)
                .releaseVersion("v1")
                .metadata(Map.of())
                .build();
    }
}