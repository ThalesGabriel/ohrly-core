package org.ohrly.core.valueObjects;

import java.time.LocalDate;
import java.util.List;

public record BehaviorPattern(
        Context context,
        LocalDate criticalDate,
        List<Double> ratiosBeforeCritical
) { }