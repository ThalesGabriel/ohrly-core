package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.FlowSensitivity;
import org.ohrly.core.enums.MetricType;
import org.ohrly.core.factory.FlowSensitivityDefaultsFactory;

import java.util.List;

public record FlowBehaviorPolicy(
        String flowId,
        String name,
        FlowSensitivity sensitivity,
        boolean requireConsecutiveness,
        BehaviorThresholds thresholds,
        List<MetricType> criticalMetrics
) {
    public static FlowBehaviorPolicy defaultFor(String flowId, String name) {
        return new FlowBehaviorPolicy(
                flowId,
                name,
                FlowSensitivity.BALANCED,
                true,
                BehaviorThresholds.defaultThresholds(),
                List.of(MetricType.APPROVAL_TIME)
        );
    }

    public BehaviorThresholds thresholds() {
        return FlowSensitivityDefaultsFactory.thresholds(sensitivity);
    }

    public int lookbackPeriods() {
        return FlowSensitivityDefaultsFactory.lookbackPeriods(sensitivity);
    }

    public int minimumVolume() {
        return FlowSensitivityDefaultsFactory.minimumVolume(sensitivity);
    }
}
