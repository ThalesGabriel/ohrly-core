package org.ohrly.core.useCases;

import org.ohrly.core.domain.ApprovalEvent;
import org.ohrly.core.domain.Order;
import org.ohrly.core.domain.Payment;
import org.ohrly.core.factory.ApprovalEventFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BuildApprovalEventsUseCase {

    @Autowired
    private ApprovalEventFactory factory;

    public List<ApprovalEvent> execute(List<Order> orders, List<Payment> payments) {
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
