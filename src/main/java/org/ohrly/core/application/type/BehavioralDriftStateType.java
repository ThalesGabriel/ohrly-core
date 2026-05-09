package org.ohrly.core.application.type;

import lombok.Getter;

@Getter
public enum BehavioralDriftStateType {

    NORMAL("Current behavior is within expected historical variation."),
    ATTENTION("Current behavior shows early signs of behavioral change."),
    DEGRADED("Current behavior shows relevant degradation compared to baseline."),
    CRITICAL("Current behavior shows severe degradation and likely operational impact.");

    private final String description;

    BehavioralDriftStateType(String description) {
        this.description = description;
    }

}
