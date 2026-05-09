package org.ohrly.core.context;

import org.ohrly.core.domain.entities.FlowDefinition;
import org.ohrly.core.domain.entities.FlowEvent;
import org.ohrly.core.domain.entities.FlowSession;
import org.ohrly.core.domain.entities.FlowStepDefinition;
import org.ohrly.core.application.type.FlowDayType;
import org.ohrly.core.domain.type.FlowSessionStatusType;
import org.ohrly.core.domain.type.FlowStepImportanceType;
import org.ohrly.core.application.type.TimeBucketType;
import org.ohrly.core.domain.valueObject.FlowContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ContextUtils {

    private static final String flowId = "second-card-copy";
    private static final String sessionId = "session-1";
    private static final Instant startedAt = Instant.parse("2026-05-04T10:00:00Z");

    public static FlowContext paymentContext(String paymentType, String timeBucket, String dayType) {
        return new FlowContext(
                "payment-approval",
                Map.of(
                        "paymentType", paymentType,
                        "timeBucket", timeBucket,
                        "dayType", dayType
                )
        );
    }

    public static FlowContext getFlowContext() {
        FlowContext cardContext = new FlowContext(
                "payment-approval",
                Map.of(
                        "paymentType", "credit_card",
                        "timeBucket", TimeBucketType.AFTERNOON,
                        "dayType", FlowDayType.BUSINESS_DAY
                )
        );
        return cardContext;
    }

    public static FlowEvent event(String stepName, Instant timestamp) {
        return FlowEvent.builder()
                .sessionId(sessionId)
                .flowId(flowId)
                .stepName(stepName)
                .status("success")
                .occurredAt(timestamp)
                .releaseVersion("v1")
                .metadata(Map.of())
                .build();
    }

    public static FlowDefinition secondCardCopyFlow() {
        return FlowDefinition.builder()
                .id(flowId)
                .name("Second Card Copy")
                .description("Fluxo de segunda via de cartão")
                .active(true)
                .steps(List.of(
                        step("intent_detected", 1, FlowStepImportanceType.MEDIUM, true),
                        step("authenticate_user", 2, FlowStepImportanceType.HIGH, true),
                        step("identify_card", 3, FlowStepImportanceType.HIGH, true),
                        step("generate_second_copy", 4, FlowStepImportanceType.HIGH, true),
                        step("deliver_response", 5, FlowStepImportanceType.HIGH, true)
                ))
                .build();
    }

    public static FlowStepDefinition step(
            String name,
            int order,
            FlowStepImportanceType importance,
            boolean required
    ) {
        return FlowStepDefinition.builder()
                .id(name)
                .name(name)
                .order(order)
                .importance(importance)
                .required(required)
                .build();
    }

    public static FlowSession createSession() {
        return FlowSession.builder().sessionId(sessionId).flowId(flowId).status(FlowSessionStatusType.OPEN).startedAt(startedAt).build();
    }

}
