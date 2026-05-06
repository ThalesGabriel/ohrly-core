package org.ohrly.core;

import org.junit.jupiter.api.Test;
import org.ohrly.core.domain.*;
import org.ohrly.core.enums.*;
import org.ohrly.core.services.ConsistencyScorerService;
import org.ohrly.core.services.FlowService;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoreApplicationTests {

	private final FlowService evaluator = new FlowService();
	private final ConsistencyScorerService scorer = new ConsistencyScorerService();

	private final String flowId = "second-card-copy";
	private final String sessionId = "session-1";
	private final Instant startedAt = Instant.parse("2026-05-04T10:00:00Z");

	@Test
	void shouldReturnHealthyScoreWhenSessionCompletesExpectedFlow() {
		var flow = secondCardCopyFlow();
		var session = createSession();

		session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
		session.addEvent(event("identify_card", startedAt.plusSeconds(10)));
		session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isTrue();
		assertThat(evaluation.getFindings()).isEmpty();

		assertThat(score.getValue()).isEqualTo(100);
		assertThat(score.getLevel()).isEqualTo(ConsistencyLevel.HEALTHY);
	}

	@Test
	void shouldReturnDegradedScoreWhenHighCriticalStepIsMissing() {
		var flow = secondCardCopyFlow();
		var session = createSession();

		session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
		// identify_card faltando
		session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isFalse();

		assertThat(evaluation.getFindings())
				.extracting(FlowFinding::type)
				.contains(FlowFindingType.MISSING_REQUIRED_STEP);

		assertThat(evaluation.getFindings())
				.anySatisfy(finding -> {
					assertThat(finding.message()).contains("identify_card");
					assertThat(finding.severity()).isEqualTo(FindingSeverity.HIGH);
				});

		assertThat(score.getValue()).isEqualTo(70);
		assertThat(score.getLevel()).isEqualTo(ConsistencyLevel.ATTENTION);
	}

	@Test
	void shouldReturnDegradedScoreWhenSessionEndsByTimeout() {
		var flow = secondCardCopyFlow();
		var session = createSession();

		session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));

		session.closeAsTimeout(startedAt.plusSeconds(300));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isFalse();

		assertThat(evaluation.getFindings())
				.extracting(FlowFinding::type)
				.contains(
						FlowFindingType.TIMEOUT,
						FlowFindingType.MISSING_REQUIRED_STEP,
						FlowFindingType.MISSING_FINAL_STEP
				);

		assertThat(score.getLevel()).isEqualTo(ConsistencyLevel.DEGRADED);
		assertThat(score.getValue()).isLessThan(70);
	}

	@Test
	void shouldPenalizeLateEventsButKeepSessionMostlyConsistent() {
		var flow = secondCardCopyFlow();
		var session = createSession();

		session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
		session.addEvent(event("identify_card", startedAt.plusSeconds(10)));
		session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		session.addEvent(event("post_completion_callback", startedAt.plusSeconds(30)));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.getFindings())
				.extracting(FlowFinding::type)
				.containsExactly(FlowFindingType.LATE_EVENTS);

		assertThat(score.getValue()).isEqualTo(90);
		assertThat(score.getLevel()).isEqualTo(ConsistencyLevel.HEALTHY);
	}

	@Test
	void shouldIgnoreInactiveStepsWhenCalculatingFinalScore() {
		var flow = FlowDefinition.builder()
				.id(flowId)
				.name("Second Card Copy")
				.description("Fluxo de segunda via de cartão")
				.active(true)
				.steps(List.of(
						step("intent_detected", 1, FlowStepImportance.MEDIUM, true),
						step("authenticate_user", 2, FlowStepImportance.HIGH, true),
						step("legacy_fraud_validation", 3, FlowStepImportance.HIGH, false),
						step("identify_card", 4, FlowStepImportance.HIGH,  true),
						step("generate_second_copy", 5, FlowStepImportance.HIGH, true),
						step("deliver_response", 6, FlowStepImportance.HIGH, true)
				))
				.build();

		var session = createSession();

		session.addEvent(event("intent_detected", startedAt.plusSeconds(1)));
		session.addEvent(event("authenticate_user", startedAt.plusSeconds(5)));
		// legacy_fraud_validation inativo e ausente
		session.addEvent(event("identify_card", startedAt.plusSeconds(10)));
		session.addEvent(event("generate_second_copy", startedAt.plusSeconds(15)));
		session.addEvent(event("deliver_response", startedAt.plusSeconds(20)));

		session.closeAsSuccess(startedAt.plusSeconds(25));

		var evaluation = evaluator.evaluateSession(flow, session);
		var score = scorer.score(evaluation);

		assertThat(evaluation.isConsistent()).isTrue();

		assertThat(evaluation.getFindings())
				.noneMatch(finding -> finding.message().contains("legacy_fraud_validation"));

		assertThat(score.getValue()).isEqualTo(100);
		assertThat(score.getLevel()).isEqualTo(ConsistencyLevel.HEALTHY);
	}

	private FlowDefinition secondCardCopyFlow() {
		return FlowDefinition.builder()
				.id(flowId)
				.name("Second Card Copy")
				.description("Fluxo de segunda via de cartão")
				.active(true)
				.steps(List.of(
						step("intent_detected", 1, FlowStepImportance.MEDIUM, true),
						step("authenticate_user", 2, FlowStepImportance.HIGH, true),
						step("identify_card", 3, FlowStepImportance.HIGH, true),
						step("generate_second_copy", 4, FlowStepImportance.HIGH, true),
						step("deliver_response", 5, FlowStepImportance.HIGH, true)
				))
				.build();
	}

	private FlowStepDefinition step(
			String name,
			int order,
			FlowStepImportance importance,
			boolean required
	) {
		return FlowStepDefinition.builder()
				.id(name)
				.name(name)
				.order(order)
				.importance(importance)
				.required(required)
				.build();
	}

	private FlowEvent event(String stepName, Instant timestamp) {
		return FlowEvent.builder()
				.sessionId(sessionId)
				.flowId(flowId)
				.stepName(stepName)
				.status("success")
				.occurredAt(timestamp)
				.releaseVersion("v1")
				.metadata(Map.of())
				.build();
	}

	private FlowSession createSession() {
		return FlowSession.builder().sessionId(sessionId).flowId(flowId).status(FlowSessionStatus.OPEN).startedAt(startedAt).build();
	}

}