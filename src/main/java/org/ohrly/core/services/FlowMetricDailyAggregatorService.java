package org.ohrly.core.services;

import org.ohrly.core.BusinessImpactExtractor;
import org.ohrly.core.domain.FlowMetricEvent;
import org.ohrly.core.enums.MetricType;
import org.ohrly.core.valueObjects.DailyFlowMetric;
import org.ohrly.core.valueObjects.FlowContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlowMetricDailyAggregatorService {

    @Autowired
    private BusinessImpactExtractor extractor;

    private record Key(
            FlowContext context,
            String metricName,
            LocalDate date
    ) {}

    public List<DailyFlowMetric> aggregate(List<FlowMetricEvent> events) {
        return events.stream()
                .collect(Collectors.groupingBy(event ->
                        new Key(
                                event.getContext(),
                                event.getMetricName(),
                                event.getOccurredAt()
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                        )
                ))
                .entrySet()
                .stream()
                .map(entry -> toMetric(entry.getKey(), entry.getValue()))
                .toList();
    }

    private DailyFlowMetric toMetric(Key key, List<FlowMetricEvent> group) {
        double averageValue = group.stream()
                .mapToDouble(FlowMetricEvent::getValue)
                .average()
                .orElse(0);

        double totalBusinessValue = group.stream()
                .mapToDouble(extractor::extract)
                .sum();

        return new DailyFlowMetric(
                key.context(),
                key.date(),
                key.metricName(),
                averageValue,
                group.size(),
                totalBusinessValue
        );
    }

}
