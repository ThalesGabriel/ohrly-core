package org.ohrly.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.domain.ApprovalEvent;
import org.ohrly.core.domain.Order;
import org.ohrly.core.domain.Payment;
import org.ohrly.core.enums.BehaviorState;
import org.ohrly.core.enums.DayType;
import org.ohrly.core.enums.TimeBucket;
import org.ohrly.core.factory.FlowBehaviorPoliciesFactory;
import org.ohrly.core.factory.FlowSensitivityDefaultsFactory;
import org.ohrly.core.infra.CsvLoader;
import org.ohrly.core.services.*;
import org.ohrly.core.useCases.BuildApprovalEventsUseCase;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CoreApplicationTests {

	private final Context ctx = new Context("credit_card", TimeBucket.AFTERNOON, DayType.BUSINESS_DAY);

	@Autowired
	private BuildApprovalEventsUseCase builder;

	@Autowired
	private CalculateBehaviorKpiUseCase calculateBehaviorKpiUseCase;

	@Test
	void shouldBuildApprovalEventsAndCalculateBaselineByContext() {
		List<Order> orders = List.of(
				new Order(
						"order-1",
						LocalDateTime.of(2024, 1, 2, 14, 0),
						LocalDateTime.of(2024, 1, 2, 14, 20)
				),
				new Order(
						"order-2",
						LocalDateTime.of(2024, 1, 2, 15, 0),
						LocalDateTime.of(2024, 1, 2, 15, 30)
				),
				new Order(
						"order-3",
						LocalDateTime.of(2024, 1, 2, 16, 0),
						LocalDateTime.of(2024, 1, 2, 16, 25)
				)
		);

		List<Payment> payments = List.of(
				new Payment("order-1", "credit_card", 0),
				new Payment("order-2", "credit_card", 0),
				new Payment("order-3", "credit_card", 0)
		);

		List<ApprovalEvent> events = builder.execute(orders, payments);

		BaselineService baselineService = new BaselineService();
		Map<Context, Baseline> baselines = baselineService.calculateBaselines(events);

		assertEquals(3, events.size());
		assertEquals(1, baselines.size());

		Baseline baseline = baselines.values().iterator().next();

		assertEquals(25.0, baseline.average());
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
	void shouldClassifyContextCorrectly() {
		Context context = ContextClassifierService.classify(
				"credit_card",
				LocalDateTime.of(2024, 1, 2, 15, 0) // terça 15h
		);

		Assertions.assertEquals("credit_card", context.paymentType());
		Assertions.assertEquals(TimeBucket.AFTERNOON, context.timeBucket());
		Assertions.assertEquals(DayType.BUSINESS_DAY, context.dayType());
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
		Context cardContext = new Context("credit_card", TimeBucket.AFTERNOON, DayType.BUSINESS_DAY);
		Context boletoContext = new Context("boleto", TimeBucket.NIGHT, DayType.WEEKEND);

		Baseline cardBaseline = new Baseline(cardContext, 25, 70);
		Baseline boletoBaseline = new Baseline(boletoContext, 360, 1440);

		long value = 120; // 2h

		boolean cardAnomaly = DeviationDetectorUtils.isAboveBaseline(value, (long) cardBaseline.average());
		boolean boletoAnomaly = DeviationDetectorUtils.isAboveBaseline(value, (long) boletoBaseline.average());

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

		List<ApprovalEvent> events = builder.execute(orders, payments);

		Map<Context, Baseline> baselines = new BaselineService()
				.calculateBaselines(events);

		Assertions.assertFalse(events.isEmpty());
		Assertions.assertFalse(baselines.isEmpty());
	}

	@Test
	void shouldAggregateEventsByContextAndDay() throws IOException {
		CsvLoader loader = new CsvLoader();

		List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
		List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

		List<ApprovalEvent> events = builder.execute(orders, payments);

		List<DailyContextMetric> metrics = new ApprovalEventDailyAggregatorService()
				.aggregate(events);

		Assertions.assertFalse(metrics.isEmpty());

		DailyContextMetric sample = metrics.getFirst();

		Assertions.assertNotNull(sample.context());
		Assertions.assertNotNull(sample.date());
		Assertions.assertTrue(sample.averageApprovalTime() > 0);
	}

	@Test
	void shouldReturnNormalWhenBehaviorIsInsideExpectedRange() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 25, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 26, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 24, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 25, 40);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.NORMAL, state);
	}

	@Test
	void shouldReturnAttentionWhenLatestValueIsSlightlyAboveExpected() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 25, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 31, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 25, 40);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.ATTENTION, state);
	}

	@Test
	void shouldReturnSustainedDegradationWhenBehaviorIsDegradedForThreeConsecutiveDays() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 25, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 40, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 45, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 4), 49, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 25, 40);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.SUSTAINED_DEGRADATION, state);
	}

	@Test
	void shouldIgnoreSingleSpikeCsv() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 25, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 60, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 26, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 25, 40);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.NORMAL, state);
	}

	@Test
	void shouldReturnPreIncidentWhenLatestValueIsTooHigh() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 25, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 55, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 25, 40);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.ATTENTION, state);
	}

	@Test
	void shouldCalculateKpisForSustainedDegradation() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 25, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 40, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 42, 120, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 4), 45, 80, 0)
		);

		Baseline baseline = new Baseline(ctx, 25, 40);

		BehaviorKpi kpi = calculateBehaviorKpiUseCase.calculate(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(ctx, kpi.context());
		Assertions.assertEquals(BehaviorState.SUSTAINED_DEGRADATION, kpi.state());

		Assertions.assertEquals(25.0, kpi.expectedAverage());
		Assertions.assertEquals(45.0, kpi.currentAverage());

		Assertions.assertEquals(20.0, kpi.deviationMinutes());
		Assertions.assertEquals(1.8, kpi.deviationRatio());

		Assertions.assertEquals(3, kpi.durationDays());
		Assertions.assertEquals(300, kpi.impactedOrders());

		Assertions.assertEquals(5140.0, kpi.excessApprovalMinutes());

		Assertions.assertTrue(kpi.message().contains("SUSTAINED_DEGRADATION"));
	}

	@Test
	void shouldReturnLowKpisWhenBehaviorIsNormal() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 24, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 25, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 26, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 25, 40);

		BehaviorKpi kpi = calculateBehaviorKpiUseCase.calculate(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.NORMAL, kpi.state());
		Assertions.assertEquals(1.0, kpi.deviationMinutes());
		Assertions.assertEquals(1.04, kpi.deviationRatio(), 0.01);
		Assertions.assertEquals(0, kpi.durationDays());
		Assertions.assertEquals(0, kpi.impactedOrders());
		Assertions.assertEquals(0, kpi.excessApprovalMinutes());
	}

	@Test
	void shouldProcessOlistCsvsAndGenerateBehaviorKpis() throws IOException {
		CsvLoader loader = new CsvLoader();

		List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
		List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

		List<ApprovalEvent> events = builder.execute(orders, payments);

		List<DailyContextMetric> dailyMetrics = new ApprovalEventDailyAggregatorService()
				.aggregate(events);

		Map<Context, Baseline> baselines = new BaselineService()
				.calculateBaselines(events);

		Map<Context, List<DailyContextMetric>> metricsByContext = dailyMetrics.stream()
				.collect(Collectors.groupingBy(DailyContextMetric::context));


		List<BehaviorKpi> kpis = metricsByContext.entrySet()
				.stream()
				.filter(entry -> baselines.containsKey(entry.getKey()))
				.map(entry -> calculateBehaviorKpiUseCase.calculate(
						entry.getValue(),
						baselines.get(entry.getKey()),
						FlowBehaviorPoliciesFactory.conservative("flow1"))
				)
				.filter(kpi -> kpi.state() != BehaviorState.NORMAL)
				.filter(kpi -> kpi.impactedOrders() >= 10)
				.filter(kpi -> kpi.excessApprovalMinutes() > 0)
				.sorted(Comparator.comparing(BehaviorKpi::excessApprovalMinutes).reversed())
				.toList();

		Assertions.assertFalse(kpis.isEmpty());

		Path outputPath = Paths.get("target/behavior_kpis.txt");

		List<String> lines = kpis.stream()
				.map(kpi -> formatKpi(kpi))
				.toList();

		Files.createDirectories(outputPath.getParent());
		Files.write(outputPath, lines);

		System.out.println("KPIs escritos em: " + outputPath.toAbsolutePath());
	}

	@Test
	void shouldReturnNormalWhenPastDegradationHasRecovered() {
		Context ctx = new Context("credit_card", TimeBucket.NIGHT, DayType.BUSINESS_DAY);

		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 500, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 520, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 540, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 4), 70, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 286, 800);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.NORMAL, state);
	}

	@Test
	void shouldReturnNormalWhenLatestAverageIsBetterThanExpectedEvenIfThereWasHistoricalDegradation() {
		Context ctx = new Context("credit_card", TimeBucket.NIGHT, DayType.BUSINESS_DAY);

		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 430, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 460, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 500, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 4), 70, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 286.43, 900);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.NORMAL, state);
	}

	@Test
	void shouldNotReturnPreIncidentForSingleCriticalSpike() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 15, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 16, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 75, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 15, 40);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.ATTENTION, state);
	}

	@Test
	void shouldReturnPreIncidentWhenCriticalBehaviorPersistsForTwoDays() {
		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 15, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 70, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 75, 100, 0)
		);

		Baseline baseline = new Baseline(ctx, 15, 40);

		BehaviorState state = new BehaviorAnalyzerService().analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertEquals(BehaviorState.PRE_INCIDENT, state);
	}

	@Test
	void shouldDetectCurrentPatternSimilarToHistoricalPreIncidentPattern() {
		Context ctx = new Context("credit_card", TimeBucket.AFTERNOON, DayType.WEEKEND);

		Baseline baseline = new Baseline(ctx, 15, 60);

		List<DailyContextMetric> historicalMetrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 18, 100, 0), // 1.2x
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 24, 100, 0), // 1.6x
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 31, 100, 0), // 2.06x
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 4), 75, 100, 0)  // crítico
		);

		List<BehaviorPattern> patterns = new BehaviorPatternExtractorService()
				.extract(historicalMetrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		List<DailyContextMetric> currentMetrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 1), 19, 100, 0), // 1.26x
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 2), 23, 100, 0), // 1.53x
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 3), 30, 100, 0)  // 2.0x
		);

		BehaviorPrecedence result = new BehaviorPrecedenceService()
				.analyze(currentMetrics, baseline, patterns, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertTrue(result.matchesHistoricalPattern());
		Assertions.assertTrue(result.similarityScore() >= 0.75);
	}

	@Test
	void shouldNotMatchDifferentCurrentPattern() {
		Context ctx = new Context("credit_card", TimeBucket.AFTERNOON,  DayType.WEEKEND);

		Baseline baseline = new Baseline(ctx, 15, 60);

		List<DailyContextMetric> historicalMetrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 18, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 24, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 31, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 4), 75, 100, 0)
		);

		List<BehaviorPattern> patterns = new BehaviorPatternExtractorService()
				.extract(historicalMetrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		List<DailyContextMetric> currentMetrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 1), 15, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 2), 16, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 3), 15, 100, 0)
		);

		BehaviorPrecedence result = new BehaviorPrecedenceService()
				.analyze(currentMetrics, baseline, patterns, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertFalse(result.matchesHistoricalPattern());
	}

	@Test
	void shouldIncludePrecedenceWhenCurrentBehaviorMatchesHistoricalCriticalPattern() {
		Context ctx = new Context("credit_card", TimeBucket.AFTERNOON, DayType.WEEKEND);

		Baseline baseline = new Baseline(ctx, 15, 60);

		List<DailyContextMetric> metrics = List.of(
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 1), 18, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 2), 24, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 3), 31, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 1, 4), 75, 100, 0),

				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 1), 19, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 2), 23, 100, 0),
				new DailyContextMetric(ctx, LocalDate.of(2024, 2, 3), 30, 100, 0)
		);

		BehaviorKpi kpi = calculateBehaviorKpiUseCase.calculate(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

		Assertions.assertNotNull(kpi.precedence());
		Assertions.assertTrue(kpi.precedence().matchesHistoricalPattern());
		Assertions.assertFalse(kpi.message().contains("episódios históricos"));
	}

	private String formatKpi(BehaviorKpi kpi) {
		return String.format(
				"Insight: %s%n" +
						"Estado: %s%n" +
						"Comportamento esperado: %.2f min%n" +
						"Comportamento atual: %.2f min%n" +
						"Desvio: %.2fx acima do esperado%n" +
						"Persistência: %d dias/períodos válidos%n" +
						"Pedidos impactados: %d%n" +
						"Valor impactado: R$ %.2f%n" +
						"Excesso acumulado: %.2f minutos%n" +
						"%s%n" +
						"------------------------------------------------------------",
				kpi.context(),
				kpi.state(),
				kpi.expectedAverage(),
				kpi.currentAverage(),
				kpi.deviationRatio(),
				kpi.durationDays(),
				kpi.impactedOrders(),
				kpi.impactedPaymentValue(),
				kpi.excessApprovalMinutes(),
				precedenceLine(kpi)
		);
	}

	private String precedenceLine(BehaviorKpi kpi) {
		if (kpi.precedence() == null || !kpi.precedence().matchesHistoricalPattern()) {
			return "";
		}

		return String.format(
				"Memória comportamental: padrão semelhante a episódios históricos críticos. Similaridade: %.2f.",
				kpi.precedence().similarityScore()
		);
	}
}