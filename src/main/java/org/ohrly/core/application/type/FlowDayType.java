package org.ohrly.core.application.type;

import org.ohrly.core.application.valueObject.DailyFlowMetric;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public enum FlowDayType {
    WEEKEND,
    BUSINESS_DAY;

    public static FlowDayType classify(LocalDateTime timestamp) {
        DayOfWeek day = timestamp.getDayOfWeek();
        return  (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                ? FlowDayType.WEEKEND
                : FlowDayType.BUSINESS_DAY;
    }

    public static FlowDayType resolveDayType(DailyFlowMetric metric) {
        Object value = metric.context().dimensions().get("dayType");

        if (value instanceof FlowDayType dayType) {
            return dayType;
        }

        if (value instanceof String text) {
            return FlowDayType.valueOf(text);
        }

        return FlowDayType.BUSINESS_DAY;
    }
}
