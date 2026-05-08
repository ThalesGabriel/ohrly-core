package org.ohrly.core.enums;

import lombok.Getter;

@Getter
public enum BehavioralConstructType {

    CLEAN_COMPLETION("Represents a healthy journey completed without relevant friction, escalation, or rupture."),
    FRICTION("Represents increased effort required to preserve functional continuity."),
    RUPTURE("Represents a functional break in the expected journey trajectory."),
    ESCALATION("Represents the inability of the primary flow to sustain the journey without alternative paths or actors."),
    RECOVERY("Represents restoration of functional continuity after failure, interruption, or degradation."),
    LOOPING_BEHAVIOR("Represents repeated execution or navigation without meaningful progression."),
    CONTINUITY_LOSS("Represents degradation in the flow's ability to sustain a coherent functional trajectory.");

    private final String description;

    BehavioralConstructType(String description) {
        this.description = description;
    }

}
