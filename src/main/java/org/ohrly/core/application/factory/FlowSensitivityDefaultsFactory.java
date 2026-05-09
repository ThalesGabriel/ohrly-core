package org.ohrly.core.application.factory;

import org.ohrly.core.application.type.FlowSensitivityType;
import org.ohrly.core.application.valueObject.BehaviorThresholds;
import org.springframework.stereotype.Component;

@Component
public class FlowSensitivityDefaultsFactory {

    public static BehaviorThresholds thresholds(FlowSensitivityType sensitivity) {
        return switch (sensitivity) {

            case CONSERVATIVE ->
                    new BehaviorThresholds(
                            4,
                            3,
                            1.3,
                            1.8,
                            2.5
                    );

            case BALANCED ->
                    new BehaviorThresholds(
                            3,
                            2,
                            1.2,
                            1.5,
                            2.0
                    );

            case AGGRESSIVE ->
                    new BehaviorThresholds(
                            2,
                            2,
                            1.2,
                            1.3,
                            1.7
                    );
        };
    }

    public static int lookbackPeriods(FlowSensitivityType sensitivity) {
        return switch (sensitivity) {

            case CONSERVATIVE -> 4;
            case BALANCED -> 3;
            case AGGRESSIVE -> 2;
        };
    }

    public static int minimumVolume(FlowSensitivityType sensitivity) {
        return switch (sensitivity) {

            case CONSERVATIVE -> 50;
            case BALANCED -> 20;
            case AGGRESSIVE -> 10;
        };
    }
}