package org.ohrly.core.services;

import org.ohrly.core.BehavioralDriftAnalyzer;
import org.ohrly.core.enums.BehavioralDriftStateType;
import org.ohrly.core.valueObjects.BehavioralDriftResult;
import org.ohrly.core.valueObjects.FlowBehaviorWindow;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SimpleBehavioralDriftAnalyzerService implements BehavioralDriftAnalyzer {

    private static final double ATTENTION_THRESHOLD = 0.10;
    private static final double DEGRADED_THRESHOLD = 0.20;
    private static final double CRITICAL_THRESHOLD = 0.35;

    @Override
    public BehavioralDriftResult analyze(
            FlowBehaviorWindow baseline,
            FlowBehaviorWindow current
    ) {
        if (baseline == null) {
            throw new IllegalArgumentException("baseline must not be null");
        }

        if (current == null) {
            throw new IllegalArgumentException("current must not be null");
        }

        if (!baseline.flowId().equals(current.flowId())) {
            throw new IllegalArgumentException("baseline and current must belong to the same flow");
        }

        var baselineSummary = baseline.summary();
        var currentSummary = current.summary();

        double frictionDelta = delta(
                baselineSummary.frictionRate(),
                currentSummary.frictionRate()
        );

        double ruptureDelta = delta(
                baselineSummary.ruptureRate(),
                currentSummary.ruptureRate()
        );

        double escalationDelta = delta(
                baselineSummary.escalationRate(),
                currentSummary.escalationRate()
        );

        double continuityLossDelta = delta(
                baselineSummary.continuityLossRate(),
                currentSummary.continuityLossRate()
        );

        double abandonDelta = delta(
                baselineSummary.abandonRate(),
                currentSummary.abandonRate()
        );

        double cleanCompletionDelta = delta(
                currentSummary.cleanCompletionRate(),
                baselineSummary.cleanCompletionRate()
        );

        double frictionScoreDelta = delta(
                baselineSummary.averageFrictionScore(),
                currentSummary.averageFrictionScore()
        ) / 10.0;

        Map<String, Double> signals = new LinkedHashMap<>();
        signals.put("frictionDelta", frictionDelta);
        signals.put("ruptureDelta", ruptureDelta);
        signals.put("escalationDelta", escalationDelta);
        signals.put("continuityLossDelta", continuityLossDelta);
        signals.put("abandonDelta", abandonDelta);
        signals.put("cleanCompletionDrop", cleanCompletionDelta);
        signals.put("averageFrictionScoreDelta", frictionScoreDelta);

        double maxSignal = signals.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        var state = classify(maxSignal);

        return new BehavioralDriftResult(
                current.flowId(),
                state,
                frictionDelta,
                ruptureDelta,
                escalationDelta,
                continuityLossDelta,
                abandonDelta,
                cleanCompletionDelta,
                frictionScoreDelta,
                signals
        );
    }

    private double delta(double baselineValue, double currentValue) {
        return currentValue - baselineValue;
    }

    private BehavioralDriftStateType classify(double maxSignal) {
        if (maxSignal >= CRITICAL_THRESHOLD) {
            return BehavioralDriftStateType.CRITICAL;
        }

        if (maxSignal >= DEGRADED_THRESHOLD) {
            return BehavioralDriftStateType.DEGRADED;
        }

        if (maxSignal >= ATTENTION_THRESHOLD) {
            return BehavioralDriftStateType.ATTENTION;
        }

        return BehavioralDriftStateType.NORMAL;
    }
}
