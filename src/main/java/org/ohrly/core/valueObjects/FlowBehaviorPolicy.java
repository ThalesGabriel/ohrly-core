package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.FlowSensitivityType;
import org.ohrly.core.enums.MetricType;
import org.ohrly.core.factory.FlowSensitivityDefaultsFactory;

import java.util.List;

public record FlowBehaviorPolicy(
        String flowId,
        String name,
        FlowSensitivityType sensitivity,
        boolean requireConsecutiveness,
        BehaviorThresholds thresholds,
        List<MetricType> criticalMetrics,
        List<String> contextPriority,
        int minimumSampleSize
) {
    public static FlowBehaviorPolicy defaultFor(String flowId, String name) {
        return new FlowBehaviorPolicy(
                flowId,
                name,
                FlowSensitivityType.BALANCED,
                true,
                BehaviorThresholds.defaultThresholds(),
                List.of(MetricType.APPROVAL_TIME),
                List.of(),
                10
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
