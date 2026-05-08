package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehaviorStateType;
import org.ohrly.core.enums.MetricType;

public record BehaviorKpi(
        FlowContext context,
        String metricName,
        BehaviorStateType state,

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

                BehaviorStateType.NORMAL,

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
