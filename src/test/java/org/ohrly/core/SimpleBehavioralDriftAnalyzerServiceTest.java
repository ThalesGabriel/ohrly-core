package org.ohrly.core;

import org.junit.jupiter.api.Test;
import org.ohrly.core.enums.BehavioralDriftStateType;
import org.ohrly.core.services.SimpleBehavioralDriftAnalyzerService;
import org.ohrly.core.valueObjects.FlowBehaviorSummary;
import org.ohrly.core.valueObjects.FlowBehaviorWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class SimpleBehavioralDriftAnalyzerServiceTest {

    @Autowired
    private SimpleBehavioralDriftAnalyzerService analyzer;

    @Test
    void shouldDetectNormalBehaviorWhenCurrentIsSimilarToBaseline() {
        var baseline = window(summary(
                100,
                0.80,
                0.05,
                0.02,
                0.03,
                0.01,
                0.01,
                0.01,
                0.0,
                1.2
        ));

        var current = window(summary(
                100,
                0.78,
                0.07,
                0.03,
                0.04,
                0.02,
                0.02,
                0.02,
                0.0,
                1.3
        ));

        var result = analyzer.analyze(baseline, current);

        assertThat(result.state()).isEqualTo(BehavioralDriftStateType.NORMAL);
        assertThat(result.isDegraded()).isFalse();
    }

    @Test
    void shouldDetectAttentionWhenFrictionIncreases() {
        var baseline = window(summary(
                100,
                0.85,
                0.05,
                0.01,
                0.02,
                0.01,
                0.01,
                0.01,
                0.0,
                1.0
        ));

        var current = window(summary(
                100,
                0.75,
                0.17,
                0.02,
                0.03,
                0.02,
                0.02,
                0.02,
                0.0,
                1.5
        ));

        var result = analyzer.analyze(baseline, current);

        assertThat(result.state()).isEqualTo(BehavioralDriftStateType.ATTENTION);
        assertThat(result.frictionDelta()).isCloseTo(0.12, within(0.0001));
    }

    @Test
    void shouldDetectDegradedBehaviorWhenContinuityLossIncreases() {
        var baseline = window(summary(
                100,
                0.88,
                0.05,
                0.02,
                0.03,
                0.02,
                0.02,
                0.01,
                0.0,
                1.0
        ));

        var current = window(summary(
                100,
                0.60,
                0.22,
                0.08,
                0.10,
                0.04, // recoveryRate
                0.25, // continuityLossRate
                0.10,
                0.02,
                2.4
        ));

        var result = analyzer.analyze(baseline, current);

        assertThat(result.state()).isEqualTo(BehavioralDriftStateType.DEGRADED);
        assertThat(result.isDegraded()).isTrue();
        assertThat(result.continuityLossDelta()).isEqualTo(0.23);
    }

    @Test
    void shouldDetectCriticalBehaviorWhenAbandonRateIncreasesSeverely() {
        var baseline = window(summary(
                100,
                0.90,
                0.05,
                0.02,
                0.02,
                0.01,
                0.01,
                0.01,
                0.0,
                1.0
        ));

        var current = window(summary(
                100,
                0.45,
                0.40,
                0.20,
                0.18,
                0.25,
                0.05,
                0.42,
                0.03,
                3.2
        ));

        var result = analyzer.analyze(baseline, current);

        assertThat(result.state()).isEqualTo(BehavioralDriftStateType.CRITICAL);
        assertThat(result.abandonDelta()).isEqualTo(0.41);
    }

    @Test
    void shouldRejectDifferentFlows() {
        var baseline = window("checkout", summary(
                10, 0.9, 0.1, 0, 0, 0, 0, 0, 0, 1
        ));

        var current = window("bill_request", summary(
                10, 0.5, 0.4, 0, 0, 0, 0, 0, 0, 3
        ));

        assertThatThrownBy(() -> analyzer.analyze(baseline, current))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseline and current must belong to the same flow");
    }

    private FlowBehaviorWindow window(FlowBehaviorSummary summary) {
        return window("bill_request", summary);
    }

    private FlowBehaviorWindow window(String flowId, FlowBehaviorSummary summary) {
        return new FlowBehaviorWindow(
                flowId,
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-02T00:00:00Z"),
                List.of(),
                summary
        );
    }

    private FlowBehaviorSummary summary(
            int totalSessions,
            double cleanCompletionRate,
            double frictionRate,
            double ruptureRate,
            double escalationRate,
            double recoveryRate,
            double continuityLossRate,
            double abandonRate,
            double timeoutRate,
            double averageFrictionScore
    ) {
        return new FlowBehaviorSummary(
                totalSessions,
                cleanCompletionRate,
                frictionRate,
                ruptureRate,
                escalationRate,
                recoveryRate,
                continuityLossRate,
                abandonRate,
                timeoutRate,
                averageFrictionScore,
                Map.of()
        );
    }
}
