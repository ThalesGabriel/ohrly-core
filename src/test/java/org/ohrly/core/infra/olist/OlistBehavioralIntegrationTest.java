package org.ohrly.core.infra.olist;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.application.valueObject.BehaviorKpi;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.ohrly.core.domain.entities.FlowMetricEvent;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.factory.FlowBehaviorPoliciesFactory;
import org.ohrly.core.infra.CsvLoader;
import org.ohrly.core.domain.service.BaselineService;
import org.ohrly.core.application.service.FlowMetricDailyAggregatorService;
import org.ohrly.core.application.useCase.CalculateBehaviorKpiUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class OlistBehavioralIntegrationTest {

    @Autowired
    private OlistApprovalMetricAdapter builder;

    @Autowired
    private BaselineService baselineService;

    @Autowired
    private FlowMetricDailyAggregatorService flowMetricDailyAggregatorService;

    @Autowired
    private CalculateBehaviorKpiUseCase calculateBehaviorKpiUseCase;

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

        List<FlowMetricEvent> metricEvents = builder.adapt(orders, payments);

        Map<FlowContext, FlowBaseline> baselines = baselineService.calculateBaselines(metricEvents);

        assertEquals(3, metricEvents.size());
        assertEquals(1, baselines.size());

        FlowBaseline baseline = baselines.values().iterator().next();

        assertEquals(25.0, baseline.expectedValue());
        assertEquals(30.0, baseline.p95());
    }

    @Test
    void shouldLoadCsvsBuildEventsAndCalculateBaselines() throws IOException {
        CsvLoader loader = new CsvLoader();
        List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
        List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

        List<FlowMetricEvent> events = builder.adapt(orders, payments);

        Map<FlowContext, FlowBaseline> baselines = baselineService.calculateBaselines(events);

        Assertions.assertFalse(events.isEmpty());
        Assertions.assertFalse(baselines.isEmpty());
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
    void shouldAggregateEventsByContextAndDay() throws IOException {
        CsvLoader loader = new CsvLoader();

        List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
        List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

        List<FlowMetricEvent> events = builder.adapt(orders, payments);

        List<DailyFlowMetric> metrics = flowMetricDailyAggregatorService.aggregate(events);

        Assertions.assertFalse(metrics.isEmpty());

        DailyFlowMetric sample = metrics.getFirst();

        Assertions.assertNotNull(sample.context());
        Assertions.assertNotNull(sample.date());
        Assertions.assertTrue(sample.averageValue() > 0);
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
    void shouldProcessOlistCsvsAndGenerateBehaviorKpis() throws IOException {
        CsvLoader loader = new CsvLoader();

        List<Order> orders = loader.loadOrders("src/test/resources/olist_orders_dataset.csv");
        List<Payment> payments = loader.loadPayments("src/test/resources/olist_order_payments_dataset.csv");

        List<FlowMetricEvent> events = builder.adapt(orders, payments);
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
                .filter(kpi -> kpi.state() != BehavioralStateType.NORMAL)
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

}
