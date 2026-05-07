package org.ohrly.core.factory;

import org.ohrly.core.domain.ApprovalEvent;
import org.ohrly.core.domain.Order;
import org.ohrly.core.domain.Payment;
import org.ohrly.core.services.ContextClassifierService;
import org.ohrly.core.valueObjects.Context;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ApprovalEventFactory {

    public ApprovalEvent create(Order order, Payment payment) {
        if (order.getApprovedAt() == null) {
            throw new IllegalArgumentException("Order has no approval timestamp");
        }

        long approvalTimeMinutes = Duration.between(
                order.getPurchaseTimestamp(),
                order.getApprovedAt()
        ).toMinutes();

        Context context = ContextClassifierService.classify(
                payment.getPaymentType(),
                order.getPurchaseTimestamp()
        );

        return new ApprovalEvent(
                order.getOrderId(),
                context,
                approvalTimeMinutes,
                order.getPurchaseTimestamp(),
                payment.getPaymentValue()
        );
    }
}
