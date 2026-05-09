package org.ohrly.core.application.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.domain.entities.FlowStepDefinition;
import org.ohrly.core.application.type.FlowFindingSeverityType;
import org.ohrly.core.application.type.FlowFindingType;
import org.ohrly.core.domain.type.FlowStepImportanceType;

@Data
@AllArgsConstructor
public class FlowFinding {

    private FlowFindingType type;
    private FlowFindingSeverityType severity;
    private String message;

    public boolean isHighSeverity() {
        return severity == FlowFindingSeverityType.HIGH;
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
                FlowFindingSeverityType.HIGH,
                "Missing final step: " + step.getName()
        );
    }

    public static FlowFinding timeout() {
        return new FlowFinding(
                FlowFindingType.TIMEOUT,
                FlowFindingSeverityType.HIGH,
                "Session ended by timeout"
        );
    }

    public static FlowFinding humanHandoff() {
        return new FlowFinding(
                FlowFindingType.HANDOFF,
                FlowFindingSeverityType.HIGH,
                "Session ended with human handoff"
        );
    }

    public static FlowFinding lateEvents(int count) {
        return new FlowFinding(
                FlowFindingType.LATE_EVENTS,
                FlowFindingSeverityType.MEDIUM,
                "Session received " + count + " late event(s) after closure"
        );
    }

    private static FlowFindingSeverityType severityFrom(FlowStepImportanceType importance) {
        return switch (importance) {
            case HIGH -> FlowFindingSeverityType.HIGH;
            case MEDIUM -> FlowFindingSeverityType.MEDIUM;
            case LOW -> FlowFindingSeverityType.LOW;
        };
    }
}