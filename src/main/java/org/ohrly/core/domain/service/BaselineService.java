package org.ohrly.core.domain.service;

import org.ohrly.core.domain.entities.FlowMetricEvent;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BaselineService {

    public Map<FlowContext, FlowBaseline> calculateBaselines(List<FlowMetricEvent> events) {
        return events.stream()
                .collect(Collectors.groupingBy(FlowMetricEvent::getContext))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateBaseline(
                                entry.getKey(),
                                entry.getValue()
                        )
                ));
    }

    private FlowBaseline calculateBaseline(FlowContext context, List<FlowMetricEvent> events) {
        List<Double> values = events.stream()
                .map(FlowMetricEvent::getValue)
                .filter(value -> value >= 0)
                .sorted()
                .toList();

        double median = percentile(values, 0.50);
        double p95 = percentile(values, 0.95);

        String metricType = events.getFirst().getMetricName();

        return new FlowBaseline(
                context,
                metricType,
                median,
                p95
        );
    }

    private double percentile(List<Double> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;

        index = Math.max(0, Math.min(index, sortedValues.size() - 1));

        return sortedValues.get(index);
    }
}