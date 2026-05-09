package org.ohrly.core.application.useCase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.application.useCase.CalculateBehaviorKpiUseCase;
import org.ohrly.core.application.valueObject.BehaviorKpi;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.ohrly.core.context.ContextUtils;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.type.MetricType;
import org.ohrly.core.application.factory.FlowBehaviorPoliciesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
public class CalculateBehaviorKpiUseCaseTest {

    @Autowired
    private CalculateBehaviorKpiUseCase calculateBehaviorKpiUseCase;

    @Test
    void shouldCalculateKpisForSustainedDegradation() {
        FlowContext ctx = ContextUtils.getFlowContext();

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
        Assertions.assertEquals(BehavioralStateType.SUSTAINED_DEGRADATION, kpi.state());

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
        FlowContext ctx = ContextUtils.getFlowContext();

        List<DailyFlowMetric> metrics = List.of(
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 24, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 26, 100, 0)
        );

        FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);

        BehaviorKpi kpi = calculateBehaviorKpiUseCase.calculate(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.NORMAL, kpi.state());
        Assertions.assertEquals(1.0, kpi.deviationValue());
        Assertions.assertEquals(1.04, kpi.deviationRatio(), 0.01);
        Assertions.assertEquals(0, kpi.durationPeriods());
        Assertions.assertEquals(0, kpi.impactedSessions());
        Assertions.assertEquals(0, kpi.excessValue());
    }

    @Test
    void shouldIncludePrecedenceWhenCurrentBehaviorMatchesHistoricalCriticalPattern() {
        FlowContext ctx = ContextUtils.paymentContext("credit_card", "AFTERNOON", "WEEKEND");
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

}
