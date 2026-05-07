package org.ohrly.core.valueObjects;

import java.time.LocalDate;

public record DailyContextMetric(
        Context context,
        LocalDate date,
        double averageApprovalTime,
        int count,
        double totalPaymentValue
) { }
