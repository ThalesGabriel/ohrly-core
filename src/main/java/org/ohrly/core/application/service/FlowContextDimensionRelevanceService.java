package org.ohrly.core.application.service;

import org.ohrly.core.domain.entities.FlowMetricEvent;
import org.ohrly.core.application.valueObject.DimensionRelevance;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FlowContextDimensionRelevanceService {

    public List<DimensionRelevance> rank(
            List<FlowMetricEvent> events,
            String metricKey
    ) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        List<FlowMetricEvent> filtered = events.stream()
                .filter(event -> metricKey.equals(event.getMetricName()))
                .filter(event -> event.getContext() != null)
                .filter(event -> event.getContext().dimensions() != null)
                .toList();

        if (filtered.size() < 2) {
            return List.of();
        }

        double totalVariance = variance(
                filtered.stream()
                        .map(FlowMetricEvent::getValue)
                        .toList()
        );

        if (totalVariance == 0) {
            return List.of();
        }

        Set<String> dimensionKeys = filtered.stream()
                .flatMap(event -> event.getContext().dimensions().keySet().stream())
                .collect(Collectors.toSet());

        return dimensionKeys.stream()
                .map(key -> relevanceFor(key, filtered, totalVariance))
                .sorted(Comparator.comparing(DimensionRelevance::explainedVarianceRatio).reversed())
                .toList();
    }

    private DimensionRelevance relevanceFor(
            String dimensionKey,
            List<FlowMetricEvent> events,
            double totalVariance
    ) {
        Map<Object, List<FlowMetricEvent>> grouped = events.stream()
                .filter(event -> event.getContext().dimensions().containsKey(dimensionKey))
                .collect(Collectors.groupingBy(
                        event -> event.getContext().dimensions().get(dimensionKey)
                ));

        double globalMean = mean(
                events.stream()
                        .map(FlowMetricEvent::getValue)
                        .toList()
        );

        int totalCount = grouped.values().stream()
                .mapToInt(List::size)
                .sum();

        double betweenGroupVariance = grouped.values()
                .stream()
                .mapToDouble(group -> {
                    double groupMean = mean(
                            group.stream()
                                    .map(FlowMetricEvent::getValue)
                                    .toList()
                    );

                    double weight = (double) group.size() / totalCount;

                    return weight * Math.pow(groupMean - globalMean, 2);
                })
                .sum();

        int minimumGroupSize = grouped.values()
                .stream()
                .mapToInt(List::size)
                .min()
                .orElse(0);

        double explainedVarianceRatio = betweenGroupVariance / totalVariance;

        return new DimensionRelevance(
                dimensionKey,
                explainedVarianceRatio,
                grouped.size(),
                minimumGroupSize
        );
    }

    private double mean(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    private double variance(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0;
        }

        double mean = mean(values);

        return values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0);
    }
}
