package org.ohrly.core.valueObjects;

import org.ohrly.core.enums.DayType;
import org.ohrly.core.enums.TimeBucket;

import java.util.Objects;

public record Context(String paymentType, TimeBucket timeBucket, DayType dayType) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Context)) return false;
        Context context = (Context) o;
        return Objects.equals(paymentType, context.paymentType) &&
                Objects.equals(timeBucket, context.timeBucket) &&
                Objects.equals(dayType, context.dayType);
    }

    @Override
    public String toString() {
        return paymentType + "-" + timeBucket + "-" + dayType;
    }
}
