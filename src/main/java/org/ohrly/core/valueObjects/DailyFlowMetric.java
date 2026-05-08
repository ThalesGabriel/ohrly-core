package org.ohrly.core.valueObjects;

import java.time.LocalDate;

public record DailyFlowMetric(
        FlowContext context,
        LocalDate date,
        String metricName,
        double averageValue,
        int count,
        double totalBusinessValue
) {}
