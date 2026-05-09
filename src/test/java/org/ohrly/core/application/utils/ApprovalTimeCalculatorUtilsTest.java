package org.ohrly.core.application.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.application.utils.ApprovalTimeCalculatorUtils;

import java.time.LocalDateTime;

public class ApprovalTimeCalculatorUtilsTest {

    @Test
    void shouldCalculateApprovalTimeInMinutes() {
        LocalDateTime purchase = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime approved = LocalDateTime.of(2024, 1, 1, 10, 30);

        long result = ApprovalTimeCalculatorUtils.calculateMinutes(purchase, approved);

        Assertions.assertEquals(30, result);
    }

}
