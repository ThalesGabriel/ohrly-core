package org.ohrly.core.services;

import org.ohrly.core.domain.ApprovalEvent;
import org.ohrly.core.valueObjects.Context;
import org.ohrly.core.valueObjects.DailyContextMetric;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalEventDailyAggregatorService {

    private record Key(Context context, LocalDate date) {}

    public List<DailyContextMetric> aggregate(List<ApprovalEvent> events) {
        return events.stream()
                .collect(Collectors.groupingBy(event ->
                        new Key(
                                event.getContext(),
                                event.getTimestamp().toLocalDate()
                        )
                ))
                .entrySet()
                .stream()
                .map(entry -> toMetric(entry.getKey(), entry.getValue()))
                .toList();
    }

    private DailyContextMetric toMetric(Key key, List<ApprovalEvent> group) {
        double averageApprovalTime = group.stream()
                .mapToLong(ApprovalEvent::getApprovalTimeMinutes)
                .average()
                .orElse(0);

        double totalPaymentValue = group.stream()
                .mapToDouble(ApprovalEvent::getPaymentValue)
                .sum();

        return new DailyContextMetric(
                key.context(),
                key.date(),
                averageApprovalTime,
                group.size(),
                totalPaymentValue
        );
    }
}
