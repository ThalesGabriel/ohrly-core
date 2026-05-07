package org.ohrly.core.factory;

import org.ohrly.core.enums.FlowSensitivity;
import org.ohrly.core.valueObjects.BehaviorThresholds;
import org.springframework.stereotype.Component;

@Component
public class FlowSensitivityDefaultsFactory {

    public static BehaviorThresholds thresholds(FlowSensitivity sensitivity) {
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

    public static int lookbackPeriods(FlowSensitivity sensitivity) {
        return switch (sensitivity) {

            case CONSERVATIVE -> 4;
            case BALANCED -> 3;
            case AGGRESSIVE -> 2;
        };
    }

    public static int minimumVolume(FlowSensitivity sensitivity) {
        return switch (sensitivity) {

            case CONSERVATIVE -> 50;
            case BALANCED -> 20;
            case AGGRESSIVE -> 10;
        };
    }
}