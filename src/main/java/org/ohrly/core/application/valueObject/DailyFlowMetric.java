package org.ohrly.core.application.valueObject;

import org.ohrly.core.domain.valueObject.FlowContext;

import java.time.LocalDate;

public record DailyFlowMetric(
        FlowContext context,
        LocalDate date,
        String metricName,
        double averageValue,
        int count,
        double totalBusinessValue
) {}
