package org.ohrly.core.application.utils;

import org.ohrly.core.application.type.FlowDayType;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class PreviousExpectedDateUtils {

    public static LocalDate calculate(LocalDate date, FlowDayType dayType) {
        if (FlowDayType.BUSINESS_DAY.equals(dayType)) {
            LocalDate previous = date.minusDays(1);

            while (previous.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    previous.getDayOfWeek() == DayOfWeek.SUNDAY) {
                previous = previous.minusDays(1);
            }

            return previous;
        }

        return date.minusDays(1);
    }

}
