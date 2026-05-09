package org.ohrly.core.application.strategies;

import org.ohrly.core.domain.entities.FlowMetricEvent;

public interface BusinessImpactExtractor {

    double extract(FlowMetricEvent event);

}