package org.ohrly.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.enums.ConsistencyLevel;

@Data
@AllArgsConstructor
public class FlowConsistencyScore {

    private String flowId;
    private String sessionId;
    private int value;
    private ConsistencyLevel level;

    public boolean isHealthy() {
        return level == ConsistencyLevel.HEALTHY;
    }

    public boolean needsAttention() {
        return level == ConsistencyLevel.ATTENTION;
    }

    public boolean isDegraded() {
        return level == ConsistencyLevel.DEGRADED;
    }
}
