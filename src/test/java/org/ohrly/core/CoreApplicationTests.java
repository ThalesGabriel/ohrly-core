package org.ohrly.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.application.domain.FlowFinding;
import org.ohrly.core.application.service.BehaviorPrecedenceService;
import org.ohrly.core.application.service.ConsistencyScorerService;
import org.ohrly.core.application.type.FlowConsistencyLevelType;
import org.ohrly.core.application.type.FlowFindingSeverityType;
import org.ohrly.core.application.type.FlowFindingType;
import org.ohrly.core.application.type.MetricType;
import org.ohrly.core.application.valueObject.*;
import org.ohrly.core.context.ContextUtils;
import org.ohrly.core.domain.entities.FlowDefinition;
import org.ohrly.core.domain.service.FlowService;
import org.ohrly.core.domain.type.FlowStepImportanceType;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.ohrly.core.application.factory.FlowBehaviorPoliciesFactory;
import org.ohrly.core.application.useCase.ExtractBehaviorPatternsUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoreApplicationTests {

	private final String flowId = "second-card-copy";
	private final Instant startedAt = Instant.parse("2026-05-04T10:00:00Z");

	@Autowired
	private FlowService evaluator;

	@Autowired
	private ConsistencyScorerService scorer;

	@Autowired
	private ExtractBehaviorPatternsUseCase extractBehaviorPatternsUseCase;

	@Autowired
	private BehaviorPrecedenceService behaviorPrecedenceService;

	@Test
	void shouldDetectCurrentPatternSimilarToHistoricalPreIncidentPattern() {
		FlowContext ctx = ContextUtils.paymentContext("credit_card", "AFTERNOON", "WEEKEND");
		FlowBehaviorPolicy policy = FlowBehaviorPoliciesFactory.balanced("flow1");

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 15, 60);

		List<DailyFlowMetric> historicalMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 18, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 24, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 31, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 75, 100, 0)
		);

		List<BehaviorPattern> patterns = extractBehaviorPatternsUseCase.extract(historicalMetrics, baseline, policy);

		List<DailyFlowMetric> currentMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 1), MetricType.APPROVAL_TIME.name(), 19, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 2), MetricType.APPROVAL_TIME.name(), 23, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 3), MetricType.APPROVAL_TIME.name(), 30, 100, 0)
		);

		BehaviorPrecedence result = behaviorPrecedenceService.analyze(currentMetrics, baseline, patterns, policy);

		Assertions.assertTrue(result.matchesHistoricalPattern());
		Assertions.assertTrue(result.similarityScore() >= 0.75);
		Assertions.assertEquals(MetricType.APPROVAL_TIME.name(), result.metricName());
	}

	@Test
	void shouldNotMatchDifferentCurrentPattern() {
		FlowContext ctx = ContextUtils.paymentContext("credit_card", "AFTERNOON", "WEEKEND");
		FlowBehaviorPolicy policy = FlowBehaviorPoliciesFactory.balanced("flow1");

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 15, 60);

		List<DailyFlowMetric> historicalMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 18, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 24, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 31, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 75, 100, 0)
		);

		List<BehaviorPattern> patterns = extractBehaviorPatternsUseCase.extract(historicalMetrics, baseline, policy);

		List<DailyFlowMetric> currentMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 1), MetricType.APPROVAL_TIME.name(), 15, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 2), MetricType.APPROVAL_TIME.name(), 16, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 3), MetricType.APPROVAL_TIME.name(), 15, 100, 0)
		);

		BehaviorPrecedence result = behaviorPrecedenceService.analyze(currentMetrics, baseline, patterns, policy);

		Assertions.assertFalse(result.matchesHistoricalPattern());
	}

	@Test
	void shouldReturnHealthyScoreWhenSessionCompletesExpectedFlow() {
		var flow = ContextUtils.secondCardCopyFlow();
		var session = ContextUtils.createSession();

		session.addEvent(ContextUtils.event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(ContextUtils.event("authenticate_user", startedAt.plusSeconds(5)));
		session.addEvent(ContextUtils.event("identify_card", startedAt.plusSeconds(10)));
		session.addEvent(ContextUtils.event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(ContextUtils.event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isTrue();
		assertThat(evaluation.getFindings()).isEmpty();

		assertThat(score.getValue()).isEqualTo(100);
		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.HEALTHY);
	}

	@Test
	void shouldReturnDegradedScoreWhenHighCriticalStepIsMissing() {
		var flow = ContextUtils.secondCardCopyFlow();
		var session = ContextUtils.createSession();

		session.addEvent(ContextUtils.event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(ContextUtils.event("authenticate_user", startedAt.plusSeconds(5)));
		// identify_card faltando
		session.addEvent(ContextUtils.event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(ContextUtils.event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isFalse();

		assertThat(evaluation.getFindings())
				.extracting(FlowFinding::getType)
				.contains(FlowFindingType.MISSING_REQUIRED_STEP);

		assertThat(evaluation.getFindings())
				.anySatisfy(finding -> {
					assertThat(finding.getMessage()).contains("identify_card");
					assertThat(finding.getSeverity()).isEqualTo(FlowFindingSeverityType.HIGH);
				});

		assertThat(score.getValue()).isEqualTo(70);
		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.ATTENTION);
	}

	@Test
	void shouldReturnDegradedScoreWhenSessionEndsByTimeout() {
		var flow = ContextUtils.secondCardCopyFlow();
		var session = ContextUtils.createSession();

		session.addEvent(ContextUtils.event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(ContextUtils.event("authenticate_user", startedAt.plusSeconds(5)));

		session.closeAsTimeout(startedAt.plusSeconds(300));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isFalse();

		assertThat(evaluation.getFindings())
				.extracting(FlowFinding::getType)
				.contains(
						FlowFindingType.TIMEOUT,
						FlowFindingType.MISSING_REQUIRED_STEP,
						FlowFindingType.MISSING_FINAL_STEP
				);

		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.DEGRADED);
		assertThat(score.getValue()).isLessThan(70);
	}

	@Test
	void shouldPenalizeLateEventsButKeepSessionMostlyConsistent() {
		var flow = ContextUtils.secondCardCopyFlow();
		var session = ContextUtils.createSession();

		session.addEvent(ContextUtils.event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(ContextUtils.event("authenticate_user", startedAt.plusSeconds(5)));
		session.addEvent(ContextUtils.event("identify_card", startedAt.plusSeconds(10)));
		session.addEvent(ContextUtils.event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(ContextUtils.event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		session.addEvent(ContextUtils.event("post_completion_callback", startedAt.plusSeconds(30)));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.getFindings())
				.extracting(FlowFinding::getType)
				.containsExactly(FlowFindingType.LATE_EVENTS);

		assertThat(score.getValue()).isEqualTo(90);
		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.HEALTHY);
	}

	@Test
	void shouldIgnoreInactiveStepsWhenCalculatingFinalScore() {
		var flow = FlowDefinition.builder()
				.id(flowId)
				.name("Second Card Copy")
				.description("Fluxo de segunda via de cartão")
				.active(true)
				.steps(List.of(
						ContextUtils.step("intent_detected", 1, FlowStepImportanceType.MEDIUM, true),
						ContextUtils.step("authenticate_user", 2, FlowStepImportanceType.HIGH, true),
						ContextUtils.step("legacy_fraud_validation", 3, FlowStepImportanceType.HIGH, false),
						ContextUtils.step("identify_card", 4, FlowStepImportanceType.HIGH,  true),
						ContextUtils.step("generate_second_copy", 5, FlowStepImportanceType.HIGH, true),
						ContextUtils.step("deliver_response", 6, FlowStepImportanceType.HIGH, true)
				))
				.build();

		var session = ContextUtils.createSession();

		session.addEvent(ContextUtils.event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(ContextUtils.event("authenticate_user", startedAt.plusSeconds(5)));
		// legacy_fraud_validation inativo e ausente
		session.addEvent(ContextUtils.event("identify_card", startedAt.plusSeconds(10)));
		session.addEvent(ContextUtils.event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(ContextUtils.event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isTrue();

		assertThat(evaluation.getFindings())
				.noneMatch(finding -> finding.getMessage().contains("legacy_fraud_validation"));

		assertThat(score.getValue()).isEqualTo(100);
		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.HEALTHY);
	}


}