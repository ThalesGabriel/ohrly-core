package org.ohrly.core.useCases;

import org.ohrly.core.domain.FlowMetricEvent;
import org.ohrly.core.olist.Order;
import org.ohrly.core.olist.Payment;
import org.ohrly.core.factory.FlowMetricEventFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BuildFlowMetricEventsUseCase {

    @Autowired
    private FlowMetricEventFactory factory;

    public List<FlowMetricEvent> execute(
            List<Order> orders,
            List<Payment> payments
    ) {

        Map<String, Payment> paymentsByOrderId = payments.stream()
                .collect(Collectors.toMap(
                        Payment::getOrderId,
                        payment -> payment,
                        (first, ignored) -> first
                ));

        return orders.stream()
                .filter(order -> order.getApprovedAt() != null)
                .filter(order -> paymentsByOrderId.containsKey(order.getOrderId()))
                .map(order -> factory.create(
                        order,
                        paymentsByOrderId.get(order.getOrderId())
                ))
                .toList();
    }
}
