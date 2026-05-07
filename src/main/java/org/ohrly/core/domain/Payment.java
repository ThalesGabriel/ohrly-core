package org.ohrly.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Payment {
    private String orderId;
    private String paymentType;
    private double paymentValue;
}
