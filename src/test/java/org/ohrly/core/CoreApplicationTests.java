package org.ohrly.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.domain.*;
import org.ohrly.core.enums.*;
import org.ohrly.core.olist.OlistApprovalMetricAdapter;
import org.ohrly.core.olist.Order;
import org.ohrly.core.olist.Payment;
import org.ohrly.core.factory.FlowBehaviorPoliciesFactory;
import org.ohrly.core.infra.CsvLoader;
import org.ohrly.core.services.*;
import org.ohrly.core.useCases.BuildFlowMetricEventsUseCase;
import org.ohrly.core.useCases.CalculateBehaviorKpiUseCase;
import org.ohrly.core.utils.ApprovalTimeCalculatorUtils;
import org.ohrly.core.utils.DeviationDetectorUtils;
import org.ohrly.core.valueObjects.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoreApplicationTests {

	private final String flowId = "second-card-copy";
	private final String sessionId = "session-1";
	private final Instant startedAt = Instant.parse("2026-05-04T10:00:00Z");

	@Autowired
	private FlowService evaluator;

	@Autowired
	private ConsistencyScorerService scorer;

	@Autowired
	private BuildFlowMetricEventsUseCase builder;

	@Autowired
	private CalculateBehaviorKpiUseCase calculateBehaviorKpiUseCase;

	@Autowired
	private OlistApprovalMetricAdapter olistApprovalMetricAdapter;

	@Autowired
	private BaselineService baselineService;

	@Autowired
	private FlowMetricDailyAggregatorService flowMetricDailyAggregatorService;

	@Autowired
	private FlowBehaviorAnalyzerService flowBehaviorAnalyzerService;


	@Test
	void shouldBuildApprovalEventsAndCalculateBaselineByContext() {
		List<Order> orders = List.of(
				new Order("order-1", LocalDateTime.of(2024, 1, 2, 14, 0), LocalDateTime.of(2024, 1, 2, 14, 20)),
				new Order("order-2", LocalDateTime.of(2024, 1, 2, 15, 0), LocalDateTime.of(2024, 1, 2, 15, 30)),
				new Order("order-3", LocalDateTime.of(2024, 1, 2, 16, 0), LocalDateTime.of(2024, 1, 2, 16, 25))
		);

		List<Payment> payments = List.of(
				new Payment("order-1", "credit_card", 0),
				new Payment("order-2", "credit_card", 0),
				new Payment("order-3", "credit_card", 0)
		);

		List<FlowMetricEvent> metricEvents = olistApprovalMetricAdapter.adapt(orders, payments);

		Map<FlowContext, FlowBaseline> baselines = baselineService.calculateBaselines(metricEvents);

		assertEquals(3, metricEvents.size());
		assertEquals(1, baselines.size());

		FlowBaseline baseline = baselines.values().iterator().next();

		assertEquals(25.0, baseline.expectedValue());
		assertEquals(30.0, baseline.p95());
	}

	@Test
	void shouldCalculateApprovalTimeInMinutes() {
		LocalDateTime purchase = LocalDateTime.of(2024, 1, 1, 10, 0);
		LocalDateTime approved = LocalDateTime.of(2024, 1, 1, 10, 30);

		long result = ApprovalTimeCalculatorUtils.calculateMinutes(purchase, approved);

		Assertions.assertEquals(30, result);
	}

	@Test
	void shouldIgnoreSingleSpike() {
		List<Long> values = List.of(25L, 27L, 80L, 26L, 24L);

		boolean result = DeviationDetectorUtils.isSustained(values, 25);

		Assertions.assertFalse(result);
	}

	@Test
	void shouldDetectSustainedDegradation() {
		List<Long> values = List.of(25L, 40L, 60L, 75L);

		boolean result = DeviationDetectorUtils.isSustained(values, 25);

		Assertions.assertTrue(result);
	}

	@Test
	void shouldTreatDifferentContextsDifferently() {
		FlowContext cardContext = getFlowContext();
		FlowContext boletoContext = getBoletoContext();

		FlowBaseline cardBaseline = new FlowBaseline(cardContext, MetricType.APPROVAL_TIME.name(), 25, 70);
		FlowBaseline boletoBaseline = new FlowBaseline(boletoContext, MetricType.APPROVAL_TIME.name(), 360, 1440);

		double value = 120; // 2h

		boolean cardAnomaly = DeviationDetectorUtils.isAboveBaseline((long) value, (long) cardBaseline.expectedValue());
		boolean boletoAnomaly = DeviationDetectorUtils.isAboveBaseline((long) value, (long) boletoBaseline.expectedValue());

		Assertions.assertTrue(cardAnomaly);
		Assertions.assertFalse(boletoAnomaly);
	}

	@Test
	void shouldLoadOrdersFromCsv() throws IOException {
		CsvLoader loader = new CsvLoader();

		List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");

		Assertions.assertEquals(99441, orders.size());
		Assertions.assertEquals("e481f51cbdc54678b7cc49136f2d6af7", orders.getFirst().getOrderId());
		Assertions.assertNotNull(orders.getFirst().getPurchaseTimestamp());
		Assertions.assertNotNull(orders.getFirst().getApprovedAt());
	}

	@Test
	void shouldLoadPaymentsFromCsv() throws IOException {
		CsvLoader loader = new CsvLoader();

		List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

		Assertions.assertEquals(103886, payments.size());
		Assertions.assertEquals("b81ef226f3fe1789b1e8b2acac839d17", payments.getFirst().getOrderId());
		Assertions.assertEquals("credit_card", payments.getFirst().getPaymentType());
	}

	@Test
	void shouldLoadCsvsBuildEventsAndCalculateBaselines() throws IOException {
		CsvLoader loader = new CsvLoader();
		List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
		List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

		List<FlowMetricEvent> events = olistApprovalMetricAdapter.adapt(orders, payments);

		Map<FlowContext, FlowBaseline> baselines = baselineService.calculateBaselines(events);

		Assertions.assertFalse(events.isEmpty());
		Assertions.assertFalse(baselines.isEmpty());
	}

	@Test
	void shouldAggregateEventsByContextAndDay() throws IOException {
		CsvLoader loader = new CsvLoader();

		List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
		List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

		List<FlowMetricEvent> events = olistApprovalMetricAdapter.adapt(orders, payments);

		List<DailyFlowMetric> metrics = flowMetricDailyAggregatorService.aggregate(events);

		Assertions.assertFalse(metrics.isEmpty());

		DailyFlowMetric sample = metrics.getFirst();

		Assertions.assertNotNull(sample.context());
		Assertions.assertNotNull(sample.date());
		Assertions.assertTrue(sample.averageValue() > 0);
	}

	@Test
	void shouldReturnNormalWhenBehaviorIsInsideExpectedRange() {
		FlowContext ctx = getFlowContext();

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 26, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 24, 100, 0)
		);

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);
		BehaviorStateType state =
				flowBehaviorAnalyzerService.analyze(
						metrics,
						baseline,
						FlowBehaviorPoliciesFactory.balanced("flow1")
				);

		Assertions.assertEquals(BehaviorStateType.NORMAL, state);
	}

	@Test
	void shouldReturnAttentionWhenLatestValueIsSlightlyAboveExpected() {

		FlowContext ctx = getFlowContext();

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 1),
						MetricType.APPROVAL_TIME.name(),
						25,
						100,
						0
				),
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 2),
						MetricType.APPROVAL_TIME.name(),
						31,
						100,
						0
				)
		);

		FlowBaseline baseline = new FlowBaseline(
				ctx,
				MetricType.APPROVAL_TIME.name(),
				25,
				40
		);

		BehaviorStateType state =
				new FlowBehaviorAnalyzerService().analyze(
						metrics,
						baseline,
						FlowBehaviorPoliciesFactory.balanced("flow1")
				);

		Assertions.assertEquals(
				BehaviorStateType.ATTENTION,
				state
		);
	}

	@Test
	void shouldReturnSustainedDegradationWhenBehaviorIsDegradedForThreeConsecutiveDays() {

		FlowContext ctx = getFlowContext();

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 1),
						MetricType.APPROVAL_TIME.name(),
						25,
						100,
						0
				),
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 2),
						MetricType.APPROVAL_TIME.name(),
						40,
						100,
						0
				),
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 3),
						MetricType.APPROVAL_TIME.name(),
						45,
						100,
						0
				),
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 4),
						MetricType.APPROVAL_TIME.name(),
						49,
						100,
						0
				)
		);

		FlowBaseline baseline = new FlowBaseline(
				ctx,
				MetricType.APPROVAL_TIME.name(),
				25,
				40
		);

		BehaviorStateType state =
				new FlowBehaviorAnalyzerService().analyze(
						metrics,
						baseline,
						FlowBehaviorPoliciesFactory.balanced("flow1")
				);

		Assertions.assertEquals(
				BehaviorStateType.SUSTAINED_DEGRADATION,
				state
		);
	}

	@Test
	void shouldIgnoreSingleSpikeCsv() {

		FlowContext ctx = getFlowContext();

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 1),
						MetricType.APPROVAL_TIME.name(),
						25,
						100,
						0
				),
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 2),
						MetricType.APPROVAL_TIME.name(),
						60,
						100,
						0
				),
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 3),
						MetricType.APPROVAL_TIME.name(),
						26,
						100,
						0
				)
		);

		FlowBaseline baseline = new FlowBaseline(
				ctx,
				MetricType.APPROVAL_TIME.name(),
				25,
				40
		);

		BehaviorStateType state =
				new FlowBehaviorAnalyzerService().analyze(
						metrics,
						baseline,
						FlowBehaviorPoliciesFactory.balanced("flow1")
				);

		Assertions.assertEquals(
				BehaviorStateType.NORMAL,
				state
		);
	}

	@Test
	void shouldReturnAttentionWhenLatestValueIsTooHigh() {

		FlowContext ctx = getFlowContext();

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 1),
						MetricType.APPROVAL_TIME.name(),
						25,
						100,
						0
				),
				new DailyFlowMetric(
						ctx,
						LocalDate.of(2024, 1, 2),
						MetricType.APPROVAL_TIME.name(),
						55,
						100,
						0
				)
		);

		FlowBaseline baseline = new FlowBaseline(
				ctx,
				MetricType.APPROVAL_TIME.name(),
				25,
				40
		);

		BehaviorStateType state =
				new FlowBehaviorAnalyzerService().analyze(
						metrics,
						baseline,
						FlowBehaviorPoliciesFactory.balanced("flow1")
				);

		Assertions.assertEquals(
				BehaviorStateType.ATTENTION,
				state
		);
	}

	@Test
	void shouldCalculateKpisForSustainedDegradation() {

		FlowContext ctx = getFlowContext();

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 40, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 42, 120, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 45, 80, 0)
		);

		FlowBaseline baseline = new FlowBaseline(
				ctx,
				MetricType.APPROVAL_TIME.name(),
				25,
				40
		);

		BehaviorKpi kpi = calculateBehaviorKpiUseCase.calculate(
				metrics,
				baseline,
				FlowBehaviorPoliciesFactory.balanced("flow1")
		);

		Assertions.assertEquals(ctx, kpi.context());
		Assertions.assertEquals(MetricType.APPROVAL_TIME.name(), kpi.metricName());
		Assertions.assertEquals(BehaviorStateType.SUSTAINED_DEGRADATION, kpi.state());

		Assertions.assertEquals(25.0, kpi.expectedValue());
		Assertions.assertEquals(45.0, kpi.currentValue());

		Assertions.assertEquals(20.0, kpi.deviationValue());
		Assertions.assertEquals(1.8, kpi.deviationRatio());

		Assertions.assertEquals(3, kpi.durationPeriods());
		Assertions.assertEquals(300, kpi.impactedSessions());

		Assertions.assertEquals(5140.0, kpi.excessValue());

		Assertions.assertTrue(kpi.message().contains("SUSTAINED_DEGRADATION"));
		Assertions.assertTrue(kpi.message().contains("APPROVAL_TIME"));
	}

	@Test
	void shouldReturnLowKpisWhenBehaviorIsNormal() {

		FlowContext ctx = new FlowContext("payment-approval",
				Map.of(
						"paymentType", "credit_card",
						"timeBucket", "AFTERNOON",
						"dayType", "BUSINESS_DAY"
				)
		);

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 24, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 26, 100, 0)
		);

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);

		BehaviorKpi kpi = calculateBehaviorKpiUseCase.calculate(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorStateType.NORMAL, kpi.state());
		Assertions.assertEquals(1.0, kpi.deviationValue());
		Assertions.assertEquals(1.04, kpi.deviationRatio(), 0.01);
		Assertions.assertEquals(0, kpi.durationPeriods());
		Assertions.assertEquals(0, kpi.impactedSessions());
		Assertions.assertEquals(0, kpi.excessValue());
	}

	@Test
	void shouldReturnNormalWhenPastDegradationHasRecovered() {
		FlowContext ctx = paymentContext("credit_card", "NIGHT", "BUSINESS_DAY");

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 500, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 520, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 540, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 70, 100, 0)
		);

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 286, 800);

		BehaviorStateType state = new FlowBehaviorAnalyzerService()
				.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorStateType.NORMAL, state);
	}

	@Test
	void shouldReturnNormalWhenLatestAverageIsBetterThanExpectedEvenIfThereWasHistoricalDegradation() {
		FlowContext ctx = paymentContext("credit_card", "NIGHT", "BUSINESS_DAY");

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 430, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 460, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 500, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 70, 100, 0)
		);

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 286.43, 900);

		BehaviorStateType state = new FlowBehaviorAnalyzerService()
				.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorStateType.NORMAL, state);
	}

	@Test
	void shouldNotReturnPreIncidentForSingleCriticalSpike() {
		FlowContext ctx = paymentContext("credit_card", "AFTERNOON", "BUSINESS_DAY");

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 15, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 16, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 75, 100, 0)
		);

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 15, 40);

		BehaviorStateType state = new FlowBehaviorAnalyzerService()
				.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorStateType.ATTENTION, state);
	}

	@Test
	void shouldReturnPreIncidentWhenCriticalBehaviorPersistsForTwoDays() {
		FlowContext ctx = paymentContext("credit_card", "AFTERNOON", "BUSINESS_DAY");

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 15, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 70, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 75, 100, 0)
		);

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 15, 40);

		BehaviorStateType state = new FlowBehaviorAnalyzerService()
				.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorStateType.PRE_INCIDENT, state);
	}

	@Test
	void shouldDetectCurrentPatternSimilarToHistoricalPreIncidentPattern() {
		FlowContext ctx = paymentContext("credit_card", "AFTERNOON", "WEEKEND");
		FlowBehaviorPolicy policy = FlowBehaviorPoliciesFactory.balanced("flow1");

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 15, 60);

		List<DailyFlowMetric> historicalMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 18, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 24, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 31, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 75, 100, 0)
		);

		List<BehaviorPattern> patterns = new BehaviorPatternExtractorService()
				.extract(historicalMetrics, baseline, policy);

		List<DailyFlowMetric> currentMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 1), MetricType.APPROVAL_TIME.name(), 19, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 2), MetricType.APPROVAL_TIME.name(), 23, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 3), MetricType.APPROVAL_TIME.name(), 30, 100, 0)
		);

		BehaviorPrecedence result = new BehaviorPrecedenceService()
				.analyze(currentMetrics, baseline, patterns, policy);

		Assertions.assertTrue(result.matchesHistoricalPattern());
		Assertions.assertTrue(result.similarityScore() >= 0.75);
		Assertions.assertEquals(MetricType.APPROVAL_TIME.name(), result.metricName());
	}

	@Test
	void shouldNotMatchDifferentCurrentPattern() {
		FlowContext ctx = paymentContext("credit_card", "AFTERNOON", "WEEKEND");
		FlowBehaviorPolicy policy = FlowBehaviorPoliciesFactory.balanced("flow1");

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 15, 60);

		List<DailyFlowMetric> historicalMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 18, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 24, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 31, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 75, 100, 0)
		);

		List<BehaviorPattern> patterns = new BehaviorPatternExtractorService()
				.extract(historicalMetrics, baseline, policy);

		List<DailyFlowMetric> currentMetrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 1), MetricType.APPROVAL_TIME.name(), 15, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 2), MetricType.APPROVAL_TIME.name(), 16, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 3), MetricType.APPROVAL_TIME.name(), 15, 100, 0)
		);

		BehaviorPrecedence result = new BehaviorPrecedenceService()
				.analyze(currentMetrics, baseline, patterns, policy);

		Assertions.assertFalse(result.matchesHistoricalPattern());
	}

	@Test
	void shouldIncludePrecedenceWhenCurrentBehaviorMatchesHistoricalCriticalPattern() {
		FlowContext ctx = paymentContext("credit_card", "AFTERNOON", "WEEKEND");
		FlowBehaviorPolicy policy = FlowBehaviorPoliciesFactory.balanced("flow1");

		FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 15, 60);

		List<DailyFlowMetric> metrics = List.of(
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 18, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 24, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 31, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 75, 100, 0),

				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 1), MetricType.APPROVAL_TIME.name(), 19, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 2), MetricType.APPROVAL_TIME.name(), 23, 100, 0),
				new DailyFlowMetric(ctx, LocalDate.of(2024, 2, 3), MetricType.APPROVAL_TIME.name(), 30, 100, 0)
		);

		BehaviorKpi kpi = calculateBehaviorKpiUseCase.calculate(metrics, baseline, policy);

		Assertions.assertNotNull(kpi.precedence());
		Assertions.assertTrue(kpi.precedence().matchesHistoricalPattern());
		Assertions.assertTrue(kpi.message().contains("degradação crítica"));
	}

	@Test
	void shouldProcessOlistCsvsAndGenerateBehaviorKpis() throws IOException {
		CsvLoader loader = new CsvLoader();

		List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
		List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

		List<FlowMetricEvent> events = olistApprovalMetricAdapter.adapt(orders, payments);
		List<DailyFlowMetric> dailyMetrics = flowMetricDailyAggregatorService.aggregate(events);
		Map<FlowContext, FlowBaseline> baselines = baselineService.calculateBaselines(events);

		Map<FlowContext, List<DailyFlowMetric>> metricsByContext = dailyMetrics.stream()
				.collect(Collectors.groupingBy(DailyFlowMetric::context));

		FlowBehaviorPolicy policy =
				FlowBehaviorPoliciesFactory.aggressive("payment-approval");

		List<BehaviorKpi> kpis = metricsByContext.entrySet()
				.stream()
				.filter(entry -> baselines.containsKey(entry.getKey()))
				.map(entry -> calculateBehaviorKpiUseCase.calculate(
						entry.getValue(),
						baselines.get(entry.getKey()),
						policy
				))
				.filter(kpi -> kpi.state() != BehaviorStateType.NORMAL)
				.filter(kpi -> kpi.impactedSessions() >= 10)
				.filter(kpi -> kpi.excessValue() > 0)
				.sorted(Comparator.comparing(BehaviorKpi::excessValue).reversed())
				.toList();

		Assertions.assertFalse(kpis.isEmpty());

		Path outputPath = Paths.get("target/behavior_kpis.txt");

		List<String> lines = kpis.stream()
				.map(this::formatKpi)
				.toList();

		Files.createDirectories(outputPath.getParent());
		Files.write(outputPath, lines);

		System.out.println("KPIs escritos em: " + outputPath.toAbsolutePath());
	}

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
		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.HEALTHY);
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
				.extracting(FlowFinding::getType)
				.contains(FlowFindingType.MISSING_REQUIRED_STEP);

		assertThat(evaluation.getFindings())
				.anySatisfy(finding -> {
					assertThat(finding.getMessage()).contains("identify_card");
					assertThat(finding.getSeverity()).isEqualTo(FindingSeverityType.HIGH);
				});

		assertThat(score.getValue()).isEqualTo(70);
		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.ATTENTION);
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
						step("intent_detected", 1, FlowStepImportanceType.MEDIUM, true),
						step("authenticate_user", 2, FlowStepImportanceType.HIGH, true),
						step("legacy_fraud_validation", 3, FlowStepImportanceType.HIGH, false),
						step("identify_card", 4, FlowStepImportanceType.HIGH,  true),
						step("generate_second_copy", 5, FlowStepImportanceType.HIGH, true),
						step("deliver_response", 6, FlowStepImportanceType.HIGH, true)
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
				.noneMatch(finding -> finding.getMessage().contains("legacy_fraud_validation"));

		assertThat(score.getValue()).isEqualTo(100);
		assertThat(score.getLevel()).isEqualTo(FlowConsistencyLevelType.HEALTHY);
	}

	private FlowDefinition secondCardCopyFlow() {
		return FlowDefinition.builder()
				.id(flowId)
				.name("Second Card Copy")
				.description("Fluxo de segunda via de cartão")
				.active(true)
				.steps(List.of(
						step("intent_detected", 1, FlowStepImportanceType.MEDIUM, true),
						step("authenticate_user", 2, FlowStepImportanceType.HIGH, true),
						step("identify_card", 3, FlowStepImportanceType.HIGH, true),
						step("generate_second_copy", 4, FlowStepImportanceType.HIGH, true),
						step("deliver_response", 5, FlowStepImportanceType.HIGH, true)
				))
				.build();
	}

	private FlowStepDefinition step(
			String name,
			int order,
			FlowStepImportanceType importance,
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
		return FlowSession.builder().sessionId(sessionId).flowId(flowId).status(FlowSessionStatusType.OPEN).startedAt(startedAt).build();
	}

	private String formatKpi(BehaviorKpi kpi) {
		return String.format(
				"Insight: %s%n" +
						"Métrica: %s%n" +
						"Estado: %s%n" +
						"Comportamento esperado: %.2f%n" +
						"Comportamento atual: %.2f%n" +
						"Desvio: %.2fx acima do esperado%n" +
						"Persistência: %d período(s) válido(s)%n" +
						"Sessões impactadas: %d%n" +
						"Valor de negócio impactado: R$ %.2f%n" +
						"Excesso acumulado: %.2f%n" +
						"%s%n" +
						"------------------------------------------------------------",
				kpi.context(),
				kpi.metricName(),
				kpi.state(),
				kpi.expectedValue(),
				kpi.currentValue(),
				kpi.deviationRatio(),
				kpi.durationPeriods(),
				kpi.impactedSessions(),
				kpi.impactedBusinessValue(),
				kpi.excessValue(),
				precedenceLine(kpi)
		);
	}

	private String precedenceLine(BehaviorKpi kpi) {
		if (kpi.precedence() == null || !kpi.precedence().matchesHistoricalPattern()) {
			return "";
		}

		return String.format(
				"Memória comportamental: padrão semelhante a episódios históricos críticos para a métrica %s. Similaridade: %.2f.",
				kpi.metricName(),
				kpi.precedence().similarityScore()
		);
	}

	private static FlowContext getBoletoContext() {
		FlowContext boletoContext = new FlowContext(
				"payment-approval",
				Map.of(
						"paymentType", "boleto",
						"timeBucket", TimeBucketType.NIGHT,
						"dayType", DayType.WEEKEND
				)
		);
		return boletoContext;
	}

	private static FlowContext getFlowContext() {
		FlowContext cardContext = new FlowContext(
				"payment-approval",
				Map.of(
						"paymentType", "credit_card",
						"timeBucket", TimeBucketType.AFTERNOON,
						"dayType", DayType.BUSINESS_DAY
				)
		);
		return cardContext;
	}

	private FlowContext paymentContext(String paymentType, String timeBucket, String dayType) {
		return new FlowContext(
				"payment-approval",
				Map.of(
						"paymentType", paymentType,
						"timeBucket", timeBucket,
						"dayType", dayType
				)
		);
	}
}