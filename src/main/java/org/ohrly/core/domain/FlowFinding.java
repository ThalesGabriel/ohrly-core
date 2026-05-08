package org.ohrly.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.enums.FindingSeverityType;
import org.ohrly.core.enums.FlowFindingType;
import org.ohrly.core.enums.FlowStepImportanceType;

@Data
@AllArgsConstructor
public class FlowFinding {

    private FlowFindingType type;
    private FindingSeverityType severity;
    private String message;

    public boolean isHighSeverity() {
        return severity == FindingSeverityType.HIGH;
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
                FindingSeverityType.HIGH,
                "Missing final step: " + step.getName()
        );
    }

    public static FlowFinding timeout() {
        return new FlowFinding(
                FlowFindingType.TIMEOUT,
                FindingSeverityType.HIGH,
                "Session ended by timeout"
        );
    }

    public static FlowFinding humanHandoff() {
        return new FlowFinding(
                FlowFindingType.HANDOFF,
                FindingSeverityType.HIGH,
                "Session ended with human handoff"
        );
    }

    public static FlowFinding lateEvents(int count) {
        return new FlowFinding(
                FlowFindingType.LATE_EVENTS,
                FindingSeverityType.MEDIUM,
                "Session received " + count + " late event(s) after closure"
        );
    }

    private static FindingSeverityType severityFrom(FlowStepImportanceType importance) {
        return switch (importance) {
            case HIGH -> FindingSeverityType.HIGH;
            case MEDIUM -> FindingSeverityType.MEDIUM;
            case LOW -> FindingSeverityType.LOW;
        };
    }
}