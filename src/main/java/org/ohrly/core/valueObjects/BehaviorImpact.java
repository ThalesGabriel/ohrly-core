package org.ohrly.core.valueObjects;

public record BehaviorImpact(
        int durationDays,
        int impactedOrders,
        double excessApprovalMinutes,
        double impactedPaymentValue
) {
    public static BehaviorImpact empty() {
        return new BehaviorImpact(0, 0, 0, 0);
    }
}
