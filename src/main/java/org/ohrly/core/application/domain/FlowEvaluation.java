package org.ohrly.core.application.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.domain.type.FlowSessionEndReasonType;

import java.util.List;

@Data
@AllArgsConstructor
public class FlowEvaluation {
    private String flowId;
    private String sessionId;
    private boolean consistent;
    private FlowSessionEndReasonType endReason;
    private List<FlowFinding> findings;
}