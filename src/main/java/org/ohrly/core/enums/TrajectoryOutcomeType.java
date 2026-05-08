package org.ohrly.core.enums;

import lombok.Getter;

@Getter
public enum TrajectoryOutcomeType {

    COMPLETED("The journey reached its intended functional outcome."),
    ABANDONED("The journey was interrupted before reaching its intended outcome."),
    TIMED_OUT("The journey exceeded the expected time limit."),
    TRANSFERRED("The journey was transferred to another actor, flow, or system."),
    OPEN("The journey is still open or unfinished."),
    UNKNOWN("The journey outcome could not be determined.");

    private final String description;

    TrajectoryOutcomeType(String description) {
        this.description = description;
    }

}
