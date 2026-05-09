package org.ohrly.core.domain.entities;

import lombok.Builder;
import lombok.Data;
import org.ohrly.core.domain.type.FlowSessionEndReasonType;
import org.ohrly.core.domain.type.FlowSessionStatusType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
@Builder
public class FlowSession {

    private final List<FlowEvent> lateEvents = new ArrayList<>();

    private String sessionId;
    private String flowId;

    private List<FlowEvent> events;

    private Instant startedAt;

    private FlowSessionStatusType status;
    private FlowSessionEndReasonType endReason;
    private Instant endedAt;

    public void addEvent(FlowEvent event) {
        if (!event.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Event belongs to another session");
        }

        if (!event.getFlowId().equals(flowId)) {
            throw new IllegalArgumentException("Event belongs to another flow");
        }

        if (isClosed()) {
            lateEvents.add(event);
            return;
        }

        if(events == null) {
            this.events = new ArrayList<>();
        }

        events.add(event);
    }

    public void closeAsSuccess(Instant endedAt) {
        close(FlowSessionEndReasonType.SUCCESS, endedAt);
    }

    public void closeAsFailure(Instant endedAt) {
        close(FlowSessionEndReasonType.FAILURE, endedAt);
    }

    public void closeAsHumanHandoff(Instant endedAt) {
        close(FlowSessionEndReasonType.HANDOFF, endedAt);
    }

    public void closeAsTimeout(Instant endedAt) {
        close(FlowSessionEndReasonType.TIMEOUT, endedAt);
    }

    private void close(FlowSessionEndReasonType reason, Instant endedAt) {
        if (isClosed()) {
            throw new IllegalStateException("Session is already closed");
        }

        this.status = FlowSessionStatusType.CLOSED;
        this.endReason = Objects.requireNonNull(reason);
        this.endedAt = Objects.requireNonNull(endedAt);
    }

    public boolean isClosed() {
        return status == FlowSessionStatusType.CLOSED;
    }

    public boolean isOpen() {
        return status == FlowSessionStatusType.OPEN;
    }

    public boolean exceededTimeout(Duration timeout, Instant now) {
        if (isClosed()) {
            return false;
        }

        return lastEvent().get().getOccurredAt().plus(timeout).isBefore(now)
                || lastEvent().get().getOccurredAt().plus(timeout).equals(now);
    }

    public List<FlowEvent> orderedEvents() {
        return events.stream()
                .sorted(Comparator.comparing(FlowEvent::getOccurredAt))
                .toList();
    }

    public Optional<FlowEvent> lastEvent() {
        return orderedEvents().stream().reduce((first, second) -> second);
    }

    public boolean containsStep(String stepName) {
        return events.stream()
                .anyMatch(event -> event.getStepName().equalsIgnoreCase(stepName));
    }

    public Duration durationUntilNow(Instant now) {
        if (isClosed()) {
            return Duration.between(startedAt, endedAt);
        }

        return Duration.between(startedAt, now);
    }
}
