package org.ohrly.core.application.useCase;

import org.ohrly.core.application.domain.FlowConsistencyScore;
import org.ohrly.core.domain.entities.FlowDefinition;
import org.ohrly.core.domain.entities.FlowSession;
import org.ohrly.core.application.factory.FlowTrajectoryFactory;
import org.ohrly.core.application.service.ConsistencyScorerService;
import org.ohrly.core.domain.service.FlowService;
import org.ohrly.core.domain.mapper.FlowSessionPrimitiveMapper;
import org.ohrly.core.domain.valueObject.BehavioralPrimitive;
import org.ohrly.core.application.valueObject.FlowExecutionAnalysis;
import org.ohrly.core.application.valueObject.FlowTrajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalyzeFlowExecutionUseCase {

    @Autowired
    private FlowService flowService;

    @Autowired
    private ConsistencyScorerService consistencyScorerService;

    @Autowired
    private FlowSessionPrimitiveMapper primitiveMapperService;

    @Autowired
    private FlowTrajectoryFactory trajectoryBuilder;

    public FlowExecutionAnalysis analyze(
            FlowDefinition flowDefinition,
            FlowSession session
    ) {
        FlowConsistencyScore consistencyResult = analyzeConsistency(
                flowDefinition,
                session
        );

        FlowTrajectory trajectory = buildTrajectory(
                flowDefinition,
                session
        );

        return new FlowExecutionAnalysis(
                flowDefinition.getId(),
                session.getSessionId(),
                consistencyResult.getValue(),
                consistencyResult,
                trajectory.outcome(),
                trajectory.constructs()
        );
    }

    private FlowConsistencyScore analyzeConsistency(
            FlowDefinition flowDefinition,
            FlowSession session
    ) {
        var findings = flowService.evaluateSession(
                flowDefinition,
                session
        );

        return consistencyScorerService.score(findings);
    }

    private FlowTrajectory buildTrajectory(
            FlowDefinition flowDefinition,
            FlowSession session
    ) {
        List<BehavioralPrimitive> primitives =
                primitiveMapperService.map(session);

        return trajectoryBuilder.create(
                flowDefinition.getId(),
                session.getSessionId(),
                primitives
        );
    }
}
