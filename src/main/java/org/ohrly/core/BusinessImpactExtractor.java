package org.ohrly.core;

import org.ohrly.core.domain.FlowMetricEvent;

public interface BusinessImpactExtractor {

    double extract(FlowMetricEvent event);

}