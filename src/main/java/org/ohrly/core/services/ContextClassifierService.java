package org.ohrly.core.services;

import org.ohrly.core.enums.DayType;
import org.ohrly.core.enums.TimeBucket;
import org.ohrly.core.valueObjects.Context;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
public class ContextClassifierService {

    public static Context classify(String paymentType, LocalDateTime timestamp) {
        TimeBucket timeBucket;
        int hour = timestamp.getHour();

        if (hour >= 6 && hour < 12) timeBucket = TimeBucket.MORNING;
        else if (hour >= 12 && hour < 18) timeBucket = TimeBucket.AFTERNOON;
        else if (hour >= 18 && hour < 24) timeBucket = TimeBucket.NIGHT;
        else timeBucket = TimeBucket.DAWN;

        DayOfWeek day = timestamp.getDayOfWeek();
        DayType dayType = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                ? DayType.WEEKEND
                : DayType.BUSINESS_DAY;

        return new Context(paymentType, timeBucket, dayType);
    }
}
