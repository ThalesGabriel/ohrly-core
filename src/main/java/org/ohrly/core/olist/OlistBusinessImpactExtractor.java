package org.ohrly.core.olist;

import org.ohrly.core.BusinessImpactExtractor;
import org.ohrly.core.domain.FlowMetricEvent;
import org.springframework.stereotype.Component;

@Component
public class OlistBusinessImpactExtractor implements BusinessImpactExtractor {

    @Override
    public double extract(FlowMetricEvent event) {

        if (event.getMetadata() == null) {
            return 0;
        }

        Object value = event.getMetadata().get("paymentValue");

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return 0;
    }
}
