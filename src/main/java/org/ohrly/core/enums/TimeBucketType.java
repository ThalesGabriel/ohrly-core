package org.ohrly.core.enums;

import java.time.LocalDateTime;

public enum TimeBucketType {
    DAWN,
    MORNING,
    AFTERNOON,
    NIGHT;

    public static TimeBucketType classify(LocalDateTime timestamp) {
        TimeBucketType timeBucketType;
        int hour = timestamp.getHour();

        if (hour >= 6 && hour < 12) timeBucketType = TimeBucketType.MORNING;
        else if (hour >= 12 && hour < 18) timeBucketType = TimeBucketType.AFTERNOON;
        else if (hour >= 18 && hour < 24) timeBucketType = TimeBucketType.NIGHT;
        else timeBucketType = TimeBucketType.DAWN;

        return timeBucketType;
    }
}
