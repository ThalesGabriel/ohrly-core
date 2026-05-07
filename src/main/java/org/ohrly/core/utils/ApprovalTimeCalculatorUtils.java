package org.ohrly.core.utils;

import java.time.Duration;
import java.time.LocalDateTime;

public class ApprovalTimeCalculatorUtils {

    public static long calculateMinutes(LocalDateTime purchase, LocalDateTime approved) {
        return Duration.between(purchase, approved).toMinutes();
    }
}