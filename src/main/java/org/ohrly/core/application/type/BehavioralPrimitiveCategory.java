package org.ohrly.core.application.type;

import lombok.Getter;

@Getter
public enum BehavioralPrimitiveCategory {

    SESSION("Represents the lifecycle of a functional session."),
    STEP("Represents movement and execution inside the expected flow."),
    FRICTION("Represents effort, repetition, delay, or loss of fluency."),
    ESCALATION("Represents transition away from the preferred path."),
    TEMPORAL_INTEGRITY("Represents temporal inconsistencies in the journey.");

    private final String description;

    BehavioralPrimitiveCategory(String description) {
        this.description = description;
    }

}
