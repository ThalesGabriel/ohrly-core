package org.ohrly.core.application.valueObject;

import org.ohrly.core.domain.valueObject.FlowContext;
import org.ohrly.core.application.type.BehavioralStateType;

public record BehaviorKpi(
        FlowContext context,
        String metricName,
        BehavioralStateType state,

        double expectedValue,
        double currentValue,

        double deviationValue,
        double deviationRatio,

        int durationPeriods,
        int impactedSessions,

        double excessValue,

        String message,

        BehaviorPrecedence precedence,

        double impactedBusinessValue
) {

    public static BehaviorKpi empty(
            FlowContext context,
            String metricName,
            double expectedValue,
            String message,
            BehaviorPrecedence precedence
    ) {

        return new BehaviorKpi(
                context,
                metricName,

                BehavioralStateType.NORMAL,

                expectedValue,
                0,

                0,
                0,

                0,
                0,

                0,

                message,

                precedence,

                0
        );
    }
}
