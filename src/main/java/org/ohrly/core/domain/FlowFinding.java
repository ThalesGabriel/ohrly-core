package org.ohrly.core.domain;

import org.ohrly.core.enums.FindingSeverity;
import org.ohrly.core.enums.FlowFindingType;
import org.ohrly.core.enums.FlowStepImportance;

public record FlowFinding(
        FlowFindingType type,
        FindingSeverity severity,
        String message
) {

    public boolean isHighSeverity() {
        return severity == FindingSeverity.HIGH;
    }

    public static FlowFinding missingRequiredStep(FlowStepDefinition step) {
        return new FlowFinding(
                FlowFindingType.MISSING_REQUIRED_STEP,
                severityFrom(step.getImportance()),
                "Missing required step: " + step.getName()
        );
    }

    public static FlowFinding missingFinalStep(FlowStepDefinition step) {
        return new FlowFinding(
                FlowFindingType.MISSING_FINAL_STEP,
                FindingSeverity.HIGH,
                "Missing final step: " + step.getName()
        );
    }

    public static FlowFinding timeout() {
        return new FlowFinding(
                FlowFindingType.TIMEOUT,
                FindingSeverity.HIGH,
                "Session ended by timeout"
        );
    }

    public static FlowFinding humanHandoff() {
        return new FlowFinding(
                FlowFindingType.HANDOFF,
                FindingSeverity.HIGH,
                "Session ended with human handoff"
        );
    }

    public static FlowFinding lateEvents(int count) {
        return new FlowFinding(
                FlowFindingType.LATE_EVENTS,
                FindingSeverity.MEDIUM,
                "Session received " + count + " late event(s) after closure"
        );
    }

    private static FindingSeverity severityFrom(FlowStepImportance importance) {
        return switch (importance) {
            case HIGH -> FindingSeverity.HIGH;
            case MEDIUM -> FindingSeverity.MEDIUM;
            case LOW -> FindingSeverity.LOW;
        };
    }
}
