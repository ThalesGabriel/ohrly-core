package org.ohrly.core.application.service;

import org.ohrly.core.application.type.BehavioralConstructType;
import org.ohrly.core.application.type.FlowOutcomeType;
import org.ohrly.core.application.valueObject.BehavioralConstruct;
import org.ohrly.core.application.valueObject.FlowBehaviorSummary;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class FlowBehaviorSummaryService {

    public FlowBehaviorSummary summarize(List<FlowTrajectory> trajectories) {
        int totalSessions = trajectories.size();

        if (totalSessions == 0) {
            return emptySummary();
        }

        Map<BehavioralConstructType, Long> constructCounts =
                countConstructs(trajectories);

        double cleanCompletionRate = rate(
                constructCounts.getOrDefault(BehavioralConstructType.CLEAN_COMPLETION, 0L),
                totalSessions
        );

        double frictionRate = rate(
                constructCounts.getOrDefault(BehavioralConstructType.FRICTION, 0L),
                totalSessions
        );

        double ruptureRate = rate(
                constructCounts.getOrDefault(BehavioralConstructType.RUPTURE, 0L),
                totalSessions
        );

        double escalationRate = rate(
                constructCounts.getOrDefault(BehavioralConstructType.ESCALATION, 0L),
                totalSessions
        );

        double recoveryRate = rate(
                constructCounts.getOrDefault(BehavioralConstructType.RECOVERY, 0L),
                totalSessions
        );

        double continuityLossRate = rate(
                constructCounts.getOrDefault(BehavioralConstructType.CONTINUITY_LOSS, 0L),
                totalSessions
        );

        double abandonRate = rateByOutcome(
                trajectories,
                FlowOutcomeType.ABANDONED
        );

        double timeoutRate = rateByOutcome(
                trajectories,
                FlowOutcomeType.TIMED_OUT
        );

        double averageFrictionScore = averageConstructScore(
                trajectories,
                BehavioralConstructType.FRICTION
        );

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
                constructCounts
        );
    }

    private FlowBehaviorSummary emptySummary() {
        return new FlowBehaviorSummary(
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                new EnumMap<>(BehavioralConstructType.class)
        );
    }

    private Map<BehavioralConstructType, Long> countConstructs(
            List<FlowTrajectory> trajectories
    ) {
        Map<BehavioralConstructType, Long> counts =
                new EnumMap<>(BehavioralConstructType.class);

        trajectories.stream()
                .flatMap(trajectory -> trajectory.constructs().stream())
                .forEach(construct ->
                        counts.merge(
                                construct.type(),
                                1L,
                                Long::sum
                        )
                );

        return counts;
    }

    private double rate(long count, int total) {
        return (double) count / total;
    }

    private double rateByOutcome(
            List<FlowTrajectory> trajectories,
            FlowOutcomeType outcome
    ) {
        long count = trajectories.stream()
                .filter(trajectory -> trajectory.outcome() == outcome)
                .count();

        return rate(count, trajectories.size());
    }

    private double averageConstructScore(
            List<FlowTrajectory> trajectories,
            BehavioralConstructType type
    ) {
        return trajectories.stream()
                .flatMap(trajectory -> trajectory.constructs().stream())
                .filter(construct -> construct.type() == type)
                .mapToDouble(BehavioralConstruct::score)
                .average()
                .orElse(0.0);
    }
}
