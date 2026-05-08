package org.ohrly.core.enums;

import lombok.Getter;

@Getter
public enum BehavioralPrimitiveType {

    START(BehavioralPrimitiveCategory.SESSION, "Represents the beginning of a functional session or journey."),
    COMPLETE(BehavioralPrimitiveCategory.SESSION, "Represents the successful functional completion of the journey or intended outcome."),
    ABANDON(BehavioralPrimitiveCategory.SESSION, "Represents a session that was interrupted or terminated before successful completion."),
    TIMEOUT(BehavioralPrimitiveCategory.SESSION, "Represents a session or step that exceeded the expected execution time limit."),
    STEP_REACHED(BehavioralPrimitiveCategory.STEP, "Indicates that a specific flow step was reached during the session."),
    STEP_COMPLETED(BehavioralPrimitiveCategory.STEP, "Represents the successful completion of a flow step."),
    STEP_FAILED(BehavioralPrimitiveCategory.STEP, "Represents a failed attempt to complete a flow step."),
    WAIT(BehavioralPrimitiveCategory.FRICTION, "Represents an abnormal or relevant waiting period between events or steps."),
    RETRY(BehavioralPrimitiveCategory.FRICTION, "Indicates that a user or system attempted the same action or step again after failure or interruption."),
    LOOP(BehavioralPrimitiveCategory.FRICTION, "Represents repetitive navigation or repeated execution of the same steps without meaningful progression."),
    FALLBACK(BehavioralPrimitiveCategory.ESCALATION, "Represents a transition from the preferred automated path to an alternative execution path."),
    HUMAN_HANDOFF(BehavioralPrimitiveCategory.ESCALATION, "Represents the transfer of the session from automation to a human operator."),
    TRANSFER(BehavioralPrimitiveCategory.ESCALATION, "Represents the transition of responsibility, context, or execution between systems, flows, or actors."),
    LATE_EVENT(BehavioralPrimitiveCategory.TEMPORAL_INTEGRITY, "Represents an event received after the session was already considered closed or completed.");

    private final BehavioralPrimitiveCategory category;
    private final String description;

    BehavioralPrimitiveType(
            BehavioralPrimitiveCategory category,
            String description
    ) {
        this.category = category;
        this.description = description;
    }

}
