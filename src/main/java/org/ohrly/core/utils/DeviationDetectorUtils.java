package org.ohrly.core.utils;

import java.util.List;

public class DeviationDetectorUtils {

    public static boolean isAboveBaseline(long value, long baseline) {
        return value > baseline * 1.5;
    }

    public static boolean isSustained(List<Long> values, long baseline) {
        int count = 0;

        for (Long v : values) {
            if (isAboveBaseline(v, baseline)) {
                count++;
            }
        }

        return count >= 3;
    }
}
