package org.ohrly.core.valueObjects;

import java.time.Instant;
import java.util.List;

public record FlowBehaviorWindow(
        String flowId,
        Instant startedAt,
        Instant finishedAt,
        List<FlowTrajectory> trajectories,
        FlowBehaviorSummary summary
) {

    public FlowBehaviorWindow {
        if (flowId == null || flowId.isBlank()) {
            throw new IllegalArgumentException("flowId must not be blank");
        }

        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }

        if (finishedAt == null) {
            throw new IllegalArgumentException("finishedAt must not be null");
        }

        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt must not be before startedAt");
        }

        trajectories = trajectories == null ? List.of() : List.copyOf(trajectories);
    }

    public int totalSessions() {
        return trajectories.size();
    }

    public boolean isEmpty() {
        return trajectories.isEmpty();
    }
}
