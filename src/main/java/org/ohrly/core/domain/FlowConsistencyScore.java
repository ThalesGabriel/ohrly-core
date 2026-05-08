package org.ohrly.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.enums.FlowConsistencyLevelType;

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