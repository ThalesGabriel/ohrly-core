package org.ohrly.core.domain.entities;

import lombok.Builder;
import lombok.Data;
import org.ohrly.core.domain.valueObject.FlowContext;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class FlowMetricEvent {

    private String metricEventId;
    private String flowId;
    private String sessionId;
    private Instant occurredAt;
    private String metricName;
    private double value;
    private FlowContext context;
    private Map<String, Object> metadata;

}
