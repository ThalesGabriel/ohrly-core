package org.ohrly.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.enums.FlowSessionEndReason;

import java.util.List;

@Data
@AllArgsConstructor
public class FlowEvaluation {
    private String flowId;
    private String sessionId;
    private boolean consistent;
    private FlowSessionEndReason endReason;
    private List<FlowFinding> findings;
}
