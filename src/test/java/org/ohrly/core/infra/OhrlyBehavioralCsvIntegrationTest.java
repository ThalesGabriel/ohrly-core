package org.ohrly.core.infra;

import org.junit.jupiter.api.Test;
import org.ohrly.core.application.valueObject.BehavioralDriftResult;
import org.ohrly.core.application.valueObject.FlowBehaviorWindow;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.ohrly.core.application.type.BehavioralDriftStateType;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.application.factory.FlowTrajectoryBuilderFactory;
import org.ohrly.core.application.factory.FlowBehaviorWindowFactory;
import org.ohrly.core.application.service.SimpleBehavioralDriftAnalyzerService;
import org.ohrly.core.application.service.FlowTrajectoryAwareBehavioralConstructExtractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OhrlyBehavioralCsvIntegrationTest {

    @Autowired
    private FlowBehaviorWindowFactory windowBuilder;

    @Autowired
    private SimpleBehavioralDriftAnalyzerService driftAnalyzer;

    @Autowired
    private FlowTrajectoryAwareBehavioralConstructExtractorService constructExtractorService;

    @Autowired
    private FlowTrajectoryBuilderFactory flowTrajectoryBuilderFactory;

    @Test
    void shouldDetectFunctionalDegradationFromCsv() throws Exception {
        List<CsvEvent> events = loadCsv("/ohrly_behavioral_dataset.csv");

        List<FlowTrajectory> trajectories = events.stream()
                .collect(Collectors.groupingBy(CsvEvent::sessionId))
                .values()
                .stream()
                .map(this::toTrajectory)
                .toList();

        List<FlowTrajectory> baselineTrajectories = trajectories.stream()
                .filter(t -> LocalDate.ofInstant(t.startedAt(), TimeZone.getDefault().toZoneId())
                        .isBefore(LocalDate.of(2026, 5, 5)))
                .toList();

        List<FlowTrajectory> currentTrajectories = trajectories.stream()
                .filter(t -> !LocalDate.ofInstant(t.startedAt(), TimeZone.getDefault().toZoneId())
                        .isBefore(LocalDate.of(2026, 5, 5)))
                .toList();

        FlowBehaviorWindow baseline = windowBuilder.build(
                "bill_request",
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-05T00:00:00Z"),
                baselineTrajectories
        );

        FlowBehaviorWindow current = windowBuilder.build(
                "bill_request",
                Instant.parse("2026-05-05T00:00:00Z"),
                Instant.parse("2026-05-08T00:00:00Z"),
                currentTrajectories
        );

        BehavioralDriftResult result =
                driftAnalyzer.analyze(baseline, current);

        assertThat(result.flowId())
                .isEqualTo("bill_request");

        assertThat(result.state())
                .isIn(
                        BehavioralDriftStateType.ATTENTION,
                        BehavioralDriftStateType.DEGRADED,
                        BehavioralDriftStateType.CRITICAL
                );

        assertThat(result.frictionDelta())
                .isGreaterThan(0);

        assertThat(result.abandonDelta())
                .isGreaterThan(0);

        assertThat(result.escalationDelta())
                .isGreaterThan(0);

        assertThat(current.summary().frictionRate())
                .isGreaterThan(baseline.summary().frictionRate());

        assertThat(current.summary().abandonRate())
                .isGreaterThan(baseline.summary().abandonRate());

        assertThat(current.summary().escalationRate())
                .isGreaterThan(baseline.summary().escalationRate());
    }

    private FlowTrajectory toTrajectory(List<CsvEvent> sessionEvents) {
        List<CsvEvent> sorted = sessionEvents.stream()
                .sorted(Comparator.comparing(CsvEvent::timestamp))
                .toList();

        String sessionId = sorted.getFirst().sessionId();
        String flowId = sorted.getFirst().flowId();

        List<BehavioralPrimitive> primitives = sorted.stream()
                .map(event -> new BehavioralPrimitive(
                        toPrimitiveType(event.eventType()),
                        sessionId,
                        event.step().isBlank() ? null : event.step(),
                        event.timestamp()
                ))
                .toList();

        return flowTrajectoryBuilderFactory.build(
                flowId,
                sessionId,
                sorted.getFirst().timestamp(),
                sorted.getLast().timestamp(),
                primitives,
                new FlowContext(flowId, Map.of())
        );
    }


    private BehavioralPrimitiveType toPrimitiveType(String value) {
        return BehavioralPrimitiveType.valueOf(value);
    }

    private List<CsvEvent> loadCsv(String path) throws Exception {
        try (var reader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream(path)
                        )
                )
        )) {
            return reader.lines()
                    .skip(1)
                    .map(this::parseLine)
                    .toList();
        }
    }

    private CsvEvent parseLine(String line) {
        String[] parts = line.split(",", -1);

        return new CsvEvent(
                parts[0],
                parts[1],
                Instant.parse(parts[2] + "Z"),
                parts[3],
                parts[4],
                parts[5],
                parts[6],
                parts[7]
        );
    }

    private record CsvEvent(
            String sessionId,
            String flowId,
            Instant timestamp,
            String eventType,
            String step,
            String channel,
            String version,
            String period
    ) {}
}
