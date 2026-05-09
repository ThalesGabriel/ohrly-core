package org.ohrly.core.application.factory;

import org.ohrly.core.application.type.FlowSensitivityType;
import org.ohrly.core.application.type.MetricType;
import org.ohrly.core.application.valueObject.BehaviorThresholds;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlowBehaviorPoliciesFactory {

    public static FlowBehaviorPolicy aggressive(String flowId) {
        return new FlowBehaviorPolicy(
                flowId,
                "Checkout",
                FlowSensitivityType.AGGRESSIVE,
                false,
                new BehaviorThresholds(
                        2,
                        2,
                        1.1,
                        1.3,
                        1.7
                ),
                List.of(
                        MetricType.APPROVAL_TIME,
                        MetricType.CONVERSION_RATE,
                        MetricType.FAILURE_RATE
                ),
                List.of(),
                10
        );
    }

    public static FlowBehaviorPolicy balanced(String flowId) {
        return new FlowBehaviorPolicy(
                flowId,
                "Payment Approval",
                FlowSensitivityType.BALANCED,
                true,
                new BehaviorThresholds(
                        3,
                        2,
                        1.2,
                        1.5,
                        2.0
                ),
                List.of(
                        MetricType.APPROVAL_TIME,
                        MetricType.RETRY_RATE,
                        MetricType.FAILURE_RATE
                ),
                List.of(),
                10
        );
    }

    public static FlowBehaviorPolicy conservative(String flowId) {
        return new FlowBehaviorPolicy(
                flowId,
                "Chatbot",
                FlowSensitivityType.CONSERVATIVE,
                true,
                new BehaviorThresholds(
                        4,
                        3,
                        1.3,
                        1.8,
                        2.5
                ),
                List.of(
                        MetricType.FALLBACK_RATE,
                        MetricType.CONVERSION_RATE
                ),
                List.of(),
                10
        );
    }
}