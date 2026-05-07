package org.ohrly.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ohrly.core.valueObjects.Context;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApprovalEvent {
    private String orderId;
    private Context context;
    private long approvalTimeMinutes;
    private LocalDateTime timestamp;
    private double paymentValue;
}
