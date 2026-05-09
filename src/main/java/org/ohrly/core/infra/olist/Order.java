package org.ohrly.core.infra.olist;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Order {

    private String orderId;
    private LocalDateTime purchaseTimestamp;
    private LocalDateTime approvedAt;

}
