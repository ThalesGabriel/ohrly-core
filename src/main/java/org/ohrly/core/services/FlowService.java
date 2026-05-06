package org.ohrly.core.services;

import org.ohrly.core.domain.*;
import org.ohrly.core.enums.FlowSessionEndReason;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FlowService {

    public FlowEvaluation evaluateSession(FlowDefinition flow, FlowSession session) {
        Objects.requireNonNull(flow);
        Objects.requireNonNull(session);

        if (!flow.getId().equals(session.getFlowId())) {
            throw new IllegalArgumentException("Session does not belong to this flow");
        }

        if (session.isOpen()) {
            throw new IllegalStateException("Cannot evaluateSession an open session");
        }

        var findings = new ArrayList<FlowFinding>();

        detectMissingRequiredSteps(flow, session, findings);
        detectMissingFinalStep(flow, session, findings);
        detectTimeout(session, findings);
        detectHandoff(session, findings);
        detectLateEvents(session, findings);

        boolean consistent = findings.stream()
                .noneMatch(FlowFinding::isHighSeverity);

        return new FlowEvaluation(
                flow.getId(),
                session.getSessionId(),
                consistent,
                session.getEndReason(),
                findings
        );
    }

    private void detectMissingRequiredSteps(
            FlowDefinition flow,
            FlowSession session,
            List<FlowFinding> findings
    ) {
        for (FlowStepDefinition step : flow.getSteps()) {
            if (step.isRequired() && !session.containsStep(step.getName())) {
                findings.add(FlowFinding.missingRequiredStep(step));
            }
        }
    }

    private void detectMissingFinalStep(
            FlowDefinition flow,
            FlowSession session,
            List<FlowFinding> findings
    ) {
        var finalStep = flow.findLastStep();

        if (finalStep.isPresent() && !session.containsStep(finalStep.get().getName())) {
            findings.add(FlowFinding.missingFinalStep(finalStep.get()));
        }
    }

    private void detectTimeout(
            FlowSession session,
            List<FlowFinding> findings
    ) {
        if (session.getEndReason() == FlowSessionEndReason.TIMEOUT) {
            findings.add(FlowFinding.timeout());
        }
    }

    private void detectHandoff(
            FlowSession session,
            List<FlowFinding> findings
    ) {
        if (session.getEndReason() == FlowSessionEndReason.HANDOFF) {
            findings.add(FlowFinding.humanHandoff());
        }
    }

    private void detectLateEvents(
            FlowSession session,
            List<FlowFinding> findings
    ) {
        if (!session.getLateEvents().isEmpty()) {
            findings.add(FlowFinding.lateEvents(session.getLateEvents().size()));
        }
    }
}
