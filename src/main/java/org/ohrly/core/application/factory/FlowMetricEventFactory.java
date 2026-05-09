package org.ohrly.core.application.factory;

import org.ohrly.core.domain.entities.FlowMetricEvent;
import org.ohrly.core.infra.olist.Order;
import org.ohrly.core.infra.olist.Payment;
import org.ohrly.core.application.type.FlowDayType;
import org.ohrly.core.application.type.MetricType;
import org.ohrly.core.application.type.TimeBucketType;
import org.ohrly.core.application.utils.ApprovalTimeCalculatorUtils;
import org.ohrly.core.domain.valueObject.FlowContext;
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
                        "dayType", FlowDayType.classify(order.getPurchaseTimestamp()).name()
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