package org.ohrly.core.services;

import org.ohrly.core.domain.ApprovalEvent;
import org.ohrly.core.valueObjects.Baseline;
import org.ohrly.core.valueObjects.Context;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BaselineService {

    public Map<Context, Baseline> calculateBaselines(List<ApprovalEvent> events) {
        return events.stream()
                .collect(Collectors.groupingBy(ApprovalEvent::getContext))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateBaseline(entry.getKey(), entry.getValue())
                ));
    }

    private Baseline calculateBaseline(Context context, List<ApprovalEvent> events) {
        List<Long> values = events.stream()
                .map(ApprovalEvent::getApprovalTimeMinutes)
                .filter(value -> value >= 0)
                .sorted()
                .toList();

        double median = percentile(values, 0.50);
        double p95 = percentile(values, 0.95);

        return new Baseline(context, median, p95);
    }

    private double percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));

        return sortedValues.get(index);
    }
}