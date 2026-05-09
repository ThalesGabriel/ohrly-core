package org.ohrly.core.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FlowExecution {

    private final String id;

    private final FlowDefinition flowDefinition;
    private final FlowSession session;

    private final String flowName;
    private final String context;

    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
}