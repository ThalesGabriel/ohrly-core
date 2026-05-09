package org.ohrly.core.application.useCase;

import org.junit.jupiter.api.Test;
import org.ohrly.core.application.type.BehavioralConstructType;
import org.ohrly.core.application.type.FlowConsistencyLevelType;
import org.ohrly.core.application.type.FlowOutcomeType;
import org.ohrly.core.application.useCase.AnalyzeFlowExecutionUseCase;
import org.ohrly.core.domain.entities.FlowDefinition;
import org.ohrly.core.domain.entities.FlowEvent;
import org.ohrly.core.domain.entities.FlowSession;
import org.ohrly.core.domain.entities.FlowStepDefinition;
import org.ohrly.core.domain.type.FlowSessionEndReasonType;
import org.ohrly.core.domain.type.FlowStepImportanceType;
import org.ohrly.core.application.valueObject.FlowExecutionAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnalyzeFlowExecutionUseCaseTest {

    @Autowired
    private AnalyzeFlowExecutionUseCase useCase;

    @Test
    void shouldAnalyzeFlowExecutionCombiningConsistencyAndTrajectory() {
        FlowDefinition flowDefinition = FlowDefinition.builder()
                .id("second_card_copy")
                .name("Second Card Copy")
                .steps(List.of(
                        FlowStepDefinition.builder()
                                .name("identify_customer")
                                .order(1)
                                .required(true)
                                .importance(FlowStepImportanceType.HIGH)
                                .build(),
                        FlowStepDefinition.builder()
                                .name("identify_card")
                                .order(2)
                                .required(true)
                                .importance(FlowStepImportanceType.HIGH)
                                .build(),
                        FlowStepDefinition.builder()
                                .name("confirm_request")
                                .order(3)
                                .required(true)
                                .importance(FlowStepImportanceType.HIGH)
                                .build()
                ))
                .build();

        FlowSession session = FlowSession.builder()
                .sessionId("session-1")
                .flowId("second_card_copy")
                .startedAt(Instant.parse("2026-05-08T10:00:00Z"))
                .endedAt(Instant.parse("2026-05-08T10:03:00Z"))
                .endedAt(Instant.now())
                .endReason(FlowSessionEndReasonType.SUCCESS)
                .events(List.of(
                        FlowEvent.builder()
                                .stepName("identify_customer")
                                .occurredAt(Instant.parse("2026-05-08T10:01:00Z"))
                                .build(),
                        FlowEvent.builder()
                                .stepName("confirm_request")
                                .occurredAt(Instant.parse("2026-05-08T10:02:00Z"))
                                .build()
                ))
                .build();

        FlowExecutionAnalysis analysis =
                useCase.analyze(flowDefinition, session);

        assertThat(analysis.flowId())
                .isEqualTo("second_card_copy");

        assertThat(analysis.sessionId())
                .isEqualTo("session-1");

        assertThat(analysis.outcome())
                .isEqualTo(FlowOutcomeType.COMPLETED);

        assertThat(analysis.consistencyScore())
                .isLessThan(100);

        assertThat(analysis.consistencyState().getLevel())
                .isIn(
                        FlowConsistencyLevelType.ATTENTION,
                        FlowConsistencyLevelType.DEGRADED
                );

        assertThat(analysis.constructs())
                .hasSize(1);

        assertThat(analysis.constructs().getFirst().type())
                .isEqualTo(BehavioralConstructType.CLEAN_COMPLETION);
    }

}