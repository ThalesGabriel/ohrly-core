package org.ohrly.core.infra.olist;

import org.ohrly.core.domain.entities.FlowMetricEvent;
import org.ohrly.core.application.type.FlowDayType;
import org.ohrly.core.application.type.MetricType;
import org.ohrly.core.application.type.TimeBucketType;
import org.ohrly.core.application.utils.ApprovalTimeCalculatorUtils;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OlistApprovalMetricAdapter {

    private static final String FLOW_ID = "payment-approval";

    public List<FlowMetricEvent> adapt(List<Order> orders, List<Payment> payments) {
        if (orders == null || orders.isEmpty() || payments == null || payments.isEmpty()) {
            return List.of();
        }

        Map<String, Payment> paymentsByOrderId = payments.stream()
                .collect(Collectors.toMap(
                        Payment::getOrderId,
                        payment -> payment,
                        (first, ignored) -> first
                ));

        return orders.stream()
                .filter(order -> order.getPurchaseTimestamp() != null)
                .filter(order -> order.getApprovedAt() != null)
                .filter(order -> paymentsByOrderId.containsKey(order.getOrderId()))
                .map(order -> toMetricEvent(order, paymentsByOrderId.get(order.getOrderId())))
                .toList();
    }

    private FlowMetricEvent toMetricEvent(Order order, Payment payment) {
        long approvalTimeMinutes = ApprovalTimeCalculatorUtils.calculateMinutes(
                order.getPurchaseTimestamp(),
                order.getApprovedAt()
        );

        var occurredAt = order.getApprovedAt().toInstant(ZoneOffset.UTC);

        Map<String, Object> dimensions = Map.of(
                "paymentType", payment.getPaymentType(),
                "timeBucket", TimeBucketType.classify(order.getPurchaseTimestamp()).name(),
                "dayType", FlowDayType.classify(order.getPurchaseTimestamp()).name()
        );

        FlowContext context = new FlowContext(
                FLOW_ID,
                dimensions
        );

        return FlowMetricEvent.builder()
                .metricEventId(UUID.randomUUID().toString())
                .flowId(FLOW_ID)
                .sessionId(order.getOrderId())
                .occurredAt(occurredAt)
                .metricName(MetricType.APPROVAL_TIME.name())
                .value(approvalTimeMinutes)
                .context(context)
                .metadata(Map.of(
                        "orderId", order.getOrderId(),
                        "paymentValue", payment.getPaymentValue()
                ))
                .build();
    }
}
