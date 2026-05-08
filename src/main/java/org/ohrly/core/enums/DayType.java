package org.ohrly.core.enums;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public enum DayType {
    WEEKEND,
    BUSINESS_DAY;

    public static DayType classify(LocalDateTime timestamp) {
        DayOfWeek day = timestamp.getDayOfWeek();
        return  (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                ? DayType.WEEKEND
                : DayType.BUSINESS_DAY;
    }
}
