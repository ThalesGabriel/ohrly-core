package org.ohrly.core.factory;

import org.ohrly.core.domain.FlowMetricEvent;
import org.ohrly.core.olist.Order;
import org.ohrly.core.olist.Payment;
import org.ohrly.core.enums.DayType;
import org.ohrly.core.enums.MetricType;
import org.ohrly.core.enums.TimeBucketType;
import org.ohrly.core.utils.ApprovalTimeCalculatorUtils;
import org.ohrly.core.valueObjects.FlowContext;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Component
public class FlowMetricEventFactory {

    public FlowMetricEvent create(
            Order order,
            Payment payment
    ) {

        long approvalTimeMinutes =
                ApprovalTimeCalculatorUtils.calculateMinutes(
                        order.getPurchaseTimestamp(),
                        order.getApprovedAt()
                );

        FlowContext context = new FlowContext(
                "payment-approval",
                Map.of(
                        "paymentType", payment.getPaymentType(),
                        "timeBucket", TimeBucketType.classify(order.getPurchaseTimestamp()).name(),
                        "dayType", DayType.classify(order.getPurchaseTimestamp()).name()
                )
        );

        return FlowMetricEvent.builder()
                .metricEventId(UUID.randomUUID().toString())
                .flowId("payment-approval")
                .sessionId(order.getOrderId())
                .occurredAt(
                        order.getApprovedAt()
                                .toInstant(ZoneOffset.UTC)
                )
                .metricName(MetricType.APPROVAL_TIME.name())
                .value(approvalTimeMinutes)
                .context(context)
                .metadata(Map.of(
                        "paymentValue", payment.getPaymentValue()
                ))
                .build();
    }
}