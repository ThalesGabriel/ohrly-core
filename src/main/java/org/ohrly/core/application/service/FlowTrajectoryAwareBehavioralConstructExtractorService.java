package org.ohrly.core.application.service;

import org.ohrly.core.application.type.BehavioralConstructType;
import org.ohrly.core.application.type.BehavioralPrimitiveType;
import org.ohrly.core.application.valueObject.BehavioralConstruct;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FlowTrajectoryAwareBehavioralConstructExtractorService {

    public List<BehavioralConstruct> extract(List<BehavioralPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty()) {
            return List.of();
        }

        var orderedPrimitives = primitives.stream()
                .sorted((a, b) -> a.timestamp().compareTo(b.timestamp()))
                .toList();

        var types = orderedPrimitives.stream()
                .map(BehavioralPrimitive::type)
                .toList();

        List<BehavioralConstruct> constructs = new ArrayList<>();

        if (hasCleanCompletion(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.CLEAN_COMPLETION,
                    matchingTypes(orderedPrimitives, Set.of(
                            BehavioralPrimitiveType.START,
                            BehavioralPrimitiveType.STEP_COMPLETED,
                            BehavioralPrimitiveType.COMPLETE
                    )),
                    1.0,
                    "Session completed successfully without relevant friction, escalation, or rupture."
            ));

            return constructs;
        }

        if (hasFriction(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.FRICTION,
                    matchingTypes(orderedPrimitives, Set.of(
                            BehavioralPrimitiveType.WAIT,
                            BehavioralPrimitiveType.RETRY,
                            BehavioralPrimitiveType.LOOP,
                            BehavioralPrimitiveType.STEP_FAILED
                    )),
                    calculateFrictionScore(types),
                    "Session required additional effort to preserve functional continuity."
            ));
        }

        if (hasRupture(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.RUPTURE,
                    matchingTypes(orderedPrimitives, Set.of(
                            BehavioralPrimitiveType.STEP_FAILED,
                            BehavioralPrimitiveType.ABANDON,
                            BehavioralPrimitiveType.TIMEOUT
                    )),
                    9.0,
                    "Session experienced a functional break after failure, timeout, or abandonment."
            ));
        }

        if (hasEscalation(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.ESCALATION,
                    matchingTypes(orderedPrimitives, Set.of(
                            BehavioralPrimitiveType.FALLBACK,
                            BehavioralPrimitiveType.HUMAN_HANDOFF,
                            BehavioralPrimitiveType.TRANSFER
                    )),
                    7.5,
                    "Session required an alternative path, actor, or system to continue."
            ));
        }

        if (hasRecovery(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.RECOVERY,
                    matchingTypes(orderedPrimitives, Set.of(
                            BehavioralPrimitiveType.STEP_FAILED,
                            BehavioralPrimitiveType.RETRY,
                            BehavioralPrimitiveType.FALLBACK,
                            BehavioralPrimitiveType.COMPLETE
                    )),
                    6.5,
                    "Session restored functional continuity after failure or interruption."
            ));
        }

        if (hasLoopingBehavior(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.LOOPING_BEHAVIOR,
                    matchingTypes(orderedPrimitives, Set.of(
                            BehavioralPrimitiveType.LOOP,
                            BehavioralPrimitiveType.RETRY
                    )),
                    7.0,
                    "Session showed repeated execution without meaningful progression."
            ));
        }

        if (hasContinuityLoss(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.CONTINUITY_LOSS,
                    matchingTypes(orderedPrimitives, Set.of(
                            BehavioralPrimitiveType.WAIT,
                            BehavioralPrimitiveType.RETRY,
                            BehavioralPrimitiveType.LOOP,
                            BehavioralPrimitiveType.FALLBACK,
                            BehavioralPrimitiveType.HUMAN_HANDOFF,
                            BehavioralPrimitiveType.ABANDON,
                            BehavioralPrimitiveType.TIMEOUT
                    )),
                    8.5,
                    "Session lost the ability to sustain a coherent functional trajectory."
            ));
        }

        return constructs;
    }

    private boolean hasCleanCompletion(List<BehavioralPrimitiveType> types) {
        return types.contains(BehavioralPrimitiveType.START)
                && types.contains(BehavioralPrimitiveType.COMPLETE)
                && !containsAny(types, Set.of(
                BehavioralPrimitiveType.STEP_FAILED,
                BehavioralPrimitiveType.WAIT,
                BehavioralPrimitiveType.RETRY,
                BehavioralPrimitiveType.LOOP,
                BehavioralPrimitiveType.FALLBACK,
                BehavioralPrimitiveType.HUMAN_HANDOFF,
                BehavioralPrimitiveType.TRANSFER,
                BehavioralPrimitiveType.ABANDON,
                BehavioralPrimitiveType.TIMEOUT
        ));
    }

    private boolean hasFriction(List<BehavioralPrimitiveType> types) {
        return containsAny(types, Set.of(
                BehavioralPrimitiveType.WAIT,
                BehavioralPrimitiveType.RETRY,
                BehavioralPrimitiveType.LOOP,
                BehavioralPrimitiveType.STEP_FAILED
        ));
    }

    private boolean hasRupture(List<BehavioralPrimitiveType> types) {
        return appearsBefore(types, BehavioralPrimitiveType.STEP_FAILED, BehavioralPrimitiveType.ABANDON)
                || appearsBefore(types, BehavioralPrimitiveType.STEP_FAILED, BehavioralPrimitiveType.TIMEOUT)
                || appearsBefore(types, BehavioralPrimitiveType.HUMAN_HANDOFF, BehavioralPrimitiveType.ABANDON);
    }

    private boolean hasEscalation(List<BehavioralPrimitiveType> types) {
        return containsAny(types, Set.of(
                BehavioralPrimitiveType.FALLBACK,
                BehavioralPrimitiveType.HUMAN_HANDOFF,
                BehavioralPrimitiveType.TRANSFER
        ));
    }

    private boolean hasRecovery(List<BehavioralPrimitiveType> types) {
        return (
                appearsBefore(types, BehavioralPrimitiveType.STEP_FAILED, BehavioralPrimitiveType.COMPLETE)
                        || appearsBefore(types, BehavioralPrimitiveType.FALLBACK, BehavioralPrimitiveType.COMPLETE)
                        || appearsBefore(types, BehavioralPrimitiveType.RETRY, BehavioralPrimitiveType.COMPLETE)
        )
                && types.contains(BehavioralPrimitiveType.COMPLETE)
                && !types.contains(BehavioralPrimitiveType.ABANDON)
                && !types.contains(BehavioralPrimitiveType.TIMEOUT);
    }

    private boolean hasLoopingBehavior(List<BehavioralPrimitiveType> types) {
        long retryCount = types.stream()
                .filter(type -> type == BehavioralPrimitiveType.RETRY)
                .count();

        return types.contains(BehavioralPrimitiveType.LOOP)
                || retryCount >= 2;
    }

    private boolean hasContinuityLoss(List<BehavioralPrimitiveType> types) {
        boolean frictionThenEscalation =
                appearsBeforeAny(
                        types,
                        Set.of(
                                BehavioralPrimitiveType.WAIT,
                                BehavioralPrimitiveType.RETRY,
                                BehavioralPrimitiveType.LOOP,
                                BehavioralPrimitiveType.STEP_FAILED
                        ),
                        Set.of(
                                BehavioralPrimitiveType.FALLBACK,
                                BehavioralPrimitiveType.HUMAN_HANDOFF,
                                BehavioralPrimitiveType.TRANSFER
                        )
                );

        boolean frictionThenBadEnd =
                appearsBeforeAny(
                        types,
                        Set.of(
                                BehavioralPrimitiveType.WAIT,
                                BehavioralPrimitiveType.RETRY,
                                BehavioralPrimitiveType.LOOP,
                                BehavioralPrimitiveType.STEP_FAILED
                        ),
                        Set.of(
                                BehavioralPrimitiveType.ABANDON,
                                BehavioralPrimitiveType.TIMEOUT
                        )
                );

        boolean escalationThenBadEnd =
                appearsBeforeAny(
                        types,
                        Set.of(
                                BehavioralPrimitiveType.FALLBACK,
                                BehavioralPrimitiveType.HUMAN_HANDOFF,
                                BehavioralPrimitiveType.TRANSFER
                        ),
                        Set.of(
                                BehavioralPrimitiveType.ABANDON,
                                BehavioralPrimitiveType.TIMEOUT
                        )
                );

        return frictionThenEscalation || frictionThenBadEnd || escalationThenBadEnd;
    }

    private boolean containsAny(List<BehavioralPrimitiveType> types, Set<BehavioralPrimitiveType> expected) {
        return types.stream().anyMatch(expected::contains);
    }

    private boolean appearsBefore(
            List<BehavioralPrimitiveType> types,
            BehavioralPrimitiveType first,
            BehavioralPrimitiveType second
    ) {
        int firstIndex = types.indexOf(first);
        int secondIndex = types.indexOf(second);

        return firstIndex >= 0 && secondIndex >= 0 && firstIndex < secondIndex;
    }

    private boolean appearsBeforeAny(
            List<BehavioralPrimitiveType> types,
            Set<BehavioralPrimitiveType> firstCandidates,
            Set<BehavioralPrimitiveType> secondCandidates
    ) {
        int firstIndex = firstIndexOfAny(types, firstCandidates);
        int secondIndex = firstIndexOfAny(types, secondCandidates);

        return firstIndex >= 0 && secondIndex >= 0 && firstIndex < secondIndex;
    }

    private int firstIndexOfAny(
            List<BehavioralPrimitiveType> types,
            Set<BehavioralPrimitiveType> candidates
    ) {
        for (int i = 0; i < types.size(); i++) {
            if (candidates.contains(types.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private List<BehavioralPrimitiveType> matchingTypes(
            List<BehavioralPrimitive> primitives,
            Set<BehavioralPrimitiveType> expectedTypes
    ) {
        return primitives.stream()
                .map(BehavioralPrimitive::type)
                .filter(expectedTypes::contains)
                .distinct()
                .toList();
    }

    private double calculateFrictionScore(List<BehavioralPrimitiveType> types) {
        double score = 0.0;

        for (BehavioralPrimitiveType type : types) {
            score += switch (type) {
                case WAIT -> 2.0;
                case RETRY -> 2.5;
                case LOOP -> 3.0;
                case STEP_FAILED -> 2.0;
                default -> 0.0;
            };
        }

        return Math.min(score, 10.0);
    }
}
