package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.BehaviorState;

public record BehaviorKpi(
        Context context,
        BehaviorState state,
        double expectedAverage,
        double currentAverage,
        double deviationMinutes,
        double deviationRatio,
        int durationDays,
        int impactedOrders,
        double excessApprovalMinutes,
        String message,
        BehaviorPrecedence precedence,
        double impactedPaymentValue
) {
    public static BehaviorKpi empty(
            Context context,
            double expectedAverage,
            String message,
            BehaviorPrecedence precedence
    ) {
        return new BehaviorKpi(
                context,
                BehaviorState.NORMAL,
                expectedAverage,
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
