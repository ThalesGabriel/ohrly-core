package org.ohrly.core.services;

import org.ohrly.core.BehavioralConstructExtractor;
import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.BehavioralPrimitiveType;
import org.ohrly.core.valueObjects.BehavioralConstruct;
import org.ohrly.core.valueObjects.BehavioralPrimitive;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RuleBasedBehavioralConstructExtractorService implements BehavioralConstructExtractor {

    @Override
    public List<BehavioralConstruct> extract(List<BehavioralPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty()) {
            return List.of();
        }

        Set<BehavioralPrimitiveType> types = primitives.stream()
                .map(BehavioralPrimitive::type)
                .collect(Collectors.toSet());

        List<BehavioralConstruct> constructs = new ArrayList<>();

        if (hasFriction(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.FRICTION,
                    matchingPrimitives(primitives, Set.of(
                            BehavioralPrimitiveType.WAIT,
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
                    matchingPrimitives(primitives, Set.of(
                            BehavioralPrimitiveType.STEP_FAILED,
                            BehavioralPrimitiveType.ABANDON,
                            BehavioralPrimitiveType.TIMEOUT
                    )),
                    9.0,
                    "Session experienced a functional break in the expected trajectory."
            ));
        }

        if (hasEscalation(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.ESCALATION,
                    matchingPrimitives(primitives, Set.of(
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
                    matchingPrimitives(primitives, Set.of(
                            BehavioralPrimitiveType.STEP_FAILED,
                            BehavioralPrimitiveType.COMPLETE
                    )),
                    6.5,
                    "Session restored functional continuity after failure or interruption."
            ));
        }

        if (hasLoopingBehavior(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.LOOPING_BEHAVIOR,
                    matchingPrimitives(primitives, Set.of(
                            BehavioralPrimitiveType.LOOP
                    )),
                    7.0,
                    "Session showed repeated navigation or execution without meaningful progression."
            ));
        }

        if (hasContinuityLoss(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.CONTINUITY_LOSS,
                    matchingPrimitives(primitives, Set.of(
                            BehavioralPrimitiveType.WAIT,
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

        if (hasCleanCompletion(types)) {
            constructs.add(new BehavioralConstruct(
                    BehavioralConstructType.CLEAN_COMPLETION,
                    matchingPrimitives(primitives, Set.of(
                            BehavioralPrimitiveType.START,
                            BehavioralPrimitiveType.STEP_COMPLETED,
                            BehavioralPrimitiveType.COMPLETE
                    )),
                    1.0,
                    "Session completed successfully without relevant friction, escalation, or rupture."
            ));
        }

        return constructs;
    }

    private boolean hasFriction(Set<BehavioralPrimitiveType> types) {
        return types.contains(BehavioralPrimitiveType.WAIT)
                || types.contains(BehavioralPrimitiveType.RETRY)
                || types.contains(BehavioralPrimitiveType.LOOP)
                || types.contains(BehavioralPrimitiveType.STEP_FAILED);
    }

    private boolean hasRupture(Set<BehavioralPrimitiveType> types) {
        return types.contains(BehavioralPrimitiveType.STEP_FAILED)
                && (types.contains(BehavioralPrimitiveType.ABANDON)
                || types.contains(BehavioralPrimitiveType.TIMEOUT));
    }

    private boolean hasEscalation(Set<BehavioralPrimitiveType> types) {
        return types.contains(BehavioralPrimitiveType.FALLBACK)
                || types.contains(BehavioralPrimitiveType.HUMAN_HANDOFF)
                || types.contains(BehavioralPrimitiveType.TRANSFER);
    }

    private boolean hasRecovery(Set<BehavioralPrimitiveType> types) {
        return types.contains(BehavioralPrimitiveType.STEP_FAILED)
                && types.contains(BehavioralPrimitiveType.RETRY)
                && types.contains(BehavioralPrimitiveType.COMPLETE);
    }

    private boolean hasLoopingBehavior(Set<BehavioralPrimitiveType> types) {
        return types.contains(BehavioralPrimitiveType.LOOP)
                || countLikeRetryPattern(types);
    }

    private boolean countLikeRetryPattern(Set<BehavioralPrimitiveType> types) {
        return types.contains(BehavioralPrimitiveType.RETRY)
                && !types.contains(BehavioralPrimitiveType.COMPLETE);
    }

    private boolean hasContinuityLoss(Set<BehavioralPrimitiveType> types) {
        boolean hasFriction = hasFriction(types);
        boolean hasEscalation = hasEscalation(types);
        boolean hasTerminationProblem = types.contains(BehavioralPrimitiveType.ABANDON)
                || types.contains(BehavioralPrimitiveType.TIMEOUT);

        return (hasFriction && hasEscalation)
                || (hasFriction && hasTerminationProblem)
                || (hasEscalation && hasTerminationProblem);
    }

    private boolean hasCleanCompletion(Set<BehavioralPrimitiveType> types) {

        boolean hasCompletion =
                types.contains(BehavioralPrimitiveType.START)
                        && types.contains(BehavioralPrimitiveType.COMPLETE);

        boolean hasFriction =
                types.contains(BehavioralPrimitiveType.WAIT)
                        || types.contains(BehavioralPrimitiveType.RETRY)
                        || types.contains(BehavioralPrimitiveType.LOOP)
                        || types.contains(BehavioralPrimitiveType.STEP_FAILED);

        boolean hasEscalation =
                types.contains(BehavioralPrimitiveType.FALLBACK)
                        || types.contains(BehavioralPrimitiveType.HUMAN_HANDOFF)
                        || types.contains(BehavioralPrimitiveType.TRANSFER);

        boolean hasRupture =
                types.contains(BehavioralPrimitiveType.ABANDON)
                        || types.contains(BehavioralPrimitiveType.TIMEOUT);

        return hasCompletion
                && !hasFriction
                && !hasEscalation
                && !hasRupture;
    }

    private List<BehavioralPrimitiveType> matchingPrimitives(
            List<BehavioralPrimitive> primitives,
            Set<BehavioralPrimitiveType> expectedTypes
    ) {
        return primitives.stream()
                .map(BehavioralPrimitive::type)
                .filter(expectedTypes::contains)
                .distinct()
                .toList();
    }

    private double calculateFrictionScore(Set<BehavioralPrimitiveType> types) {
        double score = 0.0;

        if (types.contains(BehavioralPrimitiveType.WAIT)) {
            score += 2.0;
        }

        if (types.contains(BehavioralPrimitiveType.RETRY)) {
            score += 2.5;
        }

        if (types.contains(BehavioralPrimitiveType.LOOP)) {
            score += 3.0;
        }

        if (types.contains(BehavioralPrimitiveType.STEP_FAILED)) {
            score += 2.0;
        }

        return Math.min(score, 10.0);
    }
}
