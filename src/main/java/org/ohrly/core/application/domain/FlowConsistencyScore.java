package org.ohrly.core.application.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.application.type.FlowConsistencyLevelType;

@Data
@AllArgsConstructor
public class FlowConsistencyScore {

    private String flowId;
    private String sessionId;
    private int value;
    private FlowConsistencyLevelType level;

    public boolean isHealthy() {
        return level == FlowConsistencyLevelType.HEALTHY;
    }

    public boolean needsAttention() {
        return level == FlowConsistencyLevelType.ATTENTION;
    }

    public boolean isDegraded() {
        return level == FlowConsistencyLevelType.DEGRADED;
    }
}