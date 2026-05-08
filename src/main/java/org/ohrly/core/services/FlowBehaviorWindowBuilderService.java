package org.ohrly.core.services;

import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.TrajectoryOutcomeType;
import org.ohrly.core.valueObjects.BehavioralConstruct;
import org.ohrly.core.valueObjects.FlowBehaviorSummary;
import org.ohrly.core.valueObjects.FlowBehaviorWindow;
import org.ohrly.core.valueObjects.FlowTrajectory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlowBehaviorWindowBuilderService {

    public FlowBehaviorWindow build(
            String flowId,
            Instant startedAt,
            Instant finishedAt,
            List<FlowTrajectory> trajectories
    ) {
        var safeTrajectories = trajectories == null
                ? List.<FlowTrajectory>of()
                : trajectories.stream()
                .sorted(Comparator.comparing(FlowTrajectory::startedAt))
                .toList();

        var summary = summarize(safeTrajectories);

        return new FlowBehaviorWindow(
                flowId,
                startedAt,
                finishedAt,
                safeTrajectories,
                summary
        );
    }

    private FlowBehaviorSummary summarize(List<FlowTrajectory> trajectories) {
        int total = trajectories.size();

        if (total == 0) {
            return new FlowBehaviorSummary(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    Map.of()
            );
        }

        var constructCounts = trajectories.stream()
                .flatMap(trajectory -> trajectory.constructs().stream())
                .collect(Collectors.groupingBy(
                        BehavioralConstruct::type,
                        Collectors.counting()
                ));

        return new FlowBehaviorSummary(
                total,
                rate(trajectories, FlowTrajectory::completedCleanly),
                rate(trajectories, FlowTrajectory::hasFriction),
                rate(trajectories, FlowTrajectory::hasRupture),
                rate(trajectories, FlowTrajectory::hasEscalation),
                rate(trajectories, FlowTrajectory::hasRecovery),
                rate(trajectories, FlowTrajectory::hasContinuityLoss),
                rate(trajectories, t -> t.outcome() == TrajectoryOutcomeType.ABANDONED),
                rate(trajectories, t -> t.outcome() == TrajectoryOutcomeType.TIMED_OUT),
                averageFrictionScore(trajectories),
                constructCounts
        );
    }

    private double rate(
            List<FlowTrajectory> trajectories,
            java.util.function.Predicate<FlowTrajectory> predicate
    ) {
        long count = trajectories.stream()
                .filter(predicate)
                .count();

        return count / (double) trajectories.size();
    }

    private double averageFrictionScore(List<FlowTrajectory> trajectories) {
        return trajectories.stream()
                .flatMap(trajectory -> trajectory.constructs().stream())
                .filter(construct -> construct.type() == BehavioralConstructType.FRICTION)
                .mapToDouble(BehavioralConstruct::score)
                .average()
                .orElse(0.0);
    }
}
