package org.ohrly.core.application.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.application.type.FlowDayType;
import org.ohrly.core.application.type.MetricType;
import org.ohrly.core.application.type.TimeBucketType;
import org.ohrly.core.application.utils.DeviationDetectorUtils;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.domain.valueObject.FlowContext;

import java.util.List;
import java.util.Map;

public class DeviationDetectorUtilsTest {

    @Test
    void shouldIgnoreSingleSpike() {
        List<Long> values = List.of(25L, 27L, 80L, 26L, 24L);

        boolean result = DeviationDetectorUtils.isSustained(values, 25);

        Assertions.assertFalse(result);
    }

    @Test
    void shouldDetectSustainedDegradation() {
        List<Long> values = List.of(25L, 40L, 60L, 75L);

        boolean result = DeviationDetectorUtils.isSustained(values, 25);

        Assertions.assertTrue(result);
    }

    @Test
    void shouldTreatDifferentContextsDifferently() {
        FlowContext cardContext = getFlowContext();
        FlowContext boletoContext = getBoletoContext();

        FlowBaseline cardBaseline = new FlowBaseline(cardContext, MetricType.APPROVAL_TIME.name(), 25, 70);
        FlowBaseline boletoBaseline = new FlowBaseline(boletoContext, MetricType.APPROVAL_TIME.name(), 360, 1440);

        double value = 120; // 2h

        boolean cardAnomaly = DeviationDetectorUtils.isAboveBaseline((long) value, (long) cardBaseline.expectedValue());
        boolean boletoAnomaly = DeviationDetectorUtils.isAboveBaseline((long) value, (long) boletoBaseline.expectedValue());

        Assertions.assertTrue(cardAnomaly);
        Assertions.assertFalse(boletoAnomaly);
    }

    private static FlowContext getBoletoContext() {
        FlowContext boletoContext = new FlowContext(
                "payment-approval",
                Map.of(
                        "paymentType", "boleto",
                        "timeBucket", TimeBucketType.NIGHT,
                        "dayType", FlowDayType.WEEKEND
                )
        );
        return boletoContext;
    }

    private static FlowContext getFlowContext() {
        FlowContext cardContext = new FlowContext(
                "payment-approval",
                Map.of(
                        "paymentType", "credit_card",
                        "timeBucket", TimeBucketType.AFTERNOON,
                        "dayType", FlowDayType.BUSINESS_DAY
                )
        );
        return cardContext;
    }
}
