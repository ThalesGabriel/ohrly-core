package org.ohrly.core.application.useCase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.application.type.BehavioralStateType;
import org.ohrly.core.application.type.FlowDayType;
import org.ohrly.core.application.type.MetricType;
import org.ohrly.core.application.type.TimeBucketType;
import org.ohrly.core.application.factory.FlowBehaviorPoliciesFactory;
import org.ohrly.core.application.useCase.FlowBehaviorAnalyzerUseCase;
import org.ohrly.core.application.valueObject.DailyFlowMetric;
import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.domain.valueObject.FlowContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class FlowBehaviorAnalyzerUseCaseTest {

    @Autowired
    private FlowBehaviorAnalyzerUseCase flowBehaviorAnalyzerUseCase;

    @Test
    void shouldReturnNormalWhenBehaviorIsInsideExpectedRange() {
        FlowContext ctx = getFlowContext();

        List<DailyFlowMetric> metrics = List.of(
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 26, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 24, 100, 0)
        );

        FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);
        BehavioralStateType state =
                flowBehaviorAnalyzerUseCase.analyze(
                        metrics,
                        baseline,
                        FlowBehaviorPoliciesFactory.balanced("flow1")
                );

        Assertions.assertEquals(BehavioralStateType.NORMAL, state);
    }

    @Test
    void shouldReturnAttentionWhenLatestValueIsSlightlyAboveExpected() {

        FlowContext ctx = getFlowContext();

        List<DailyFlowMetric> metrics = List.of(
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 31, 100, 0)
        );

        FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);

        BehavioralStateType state =
                flowBehaviorAnalyzerUseCase.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.ATTENTION, state);
    }

    @Test
    void shouldReturnSustainedDegradationWhenBehaviorIsDegradedForThreeConsecutiveDays() {
        FlowContext ctx = getFlowContext();

        List<DailyFlowMetric> metrics = List.of(
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 40, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 45, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 4), MetricType.APPROVAL_TIME.name(), 49, 100, 0)
        );

        FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);

        BehavioralStateType state =
                flowBehaviorAnalyzerUseCase.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.SUSTAINED_DEGRADATION, state);
    }

    @Test
    void shouldIgnoreSingleSpikeCsv() {

        FlowContext ctx = getFlowContext();

        List<DailyFlowMetric> metrics = List.of(
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 60, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 3), MetricType.APPROVAL_TIME.name(), 26, 100, 0)
        );

        FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);

        BehavioralStateType state =
                flowBehaviorAnalyzerUseCase.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.NORMAL, state);
    }

    @Test
    void shouldReturnAttentionWhenLatestValueIsTooHigh() {

        FlowContext ctx = getFlowContext();

        List<DailyFlowMetric> metrics = List.of(
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 1), MetricType.APPROVAL_TIME.name(), 25, 100, 0),
                new DailyFlowMetric(ctx, LocalDate.of(2024, 1, 2), MetricType.APPROVAL_TIME.name(), 55, 100, 0)
        );

        FlowBaseline baseline = new FlowBaseline(ctx, MetricType.APPROVAL_TIME.name(), 25, 40);

        BehavioralStateType state =
                flowBehaviorAnalyzerUseCase.analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.ATTENTION, state);
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

        BehavioralStateType state = flowBehaviorAnalyzerUseCase
                .analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.NORMAL, state);
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

        BehavioralStateType state = flowBehaviorAnalyzerUseCase
                .analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.NORMAL, state);
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

        BehavioralStateType state = flowBehaviorAnalyzerUseCase
                .analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.ATTENTION, state);
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

        BehavioralStateType state = flowBehaviorAnalyzerUseCase
                .analyze(metrics, baseline, FlowBehaviorPoliciesFactory.balanced("flow1"));

        Assertions.assertEquals(BehavioralStateType.PRE_INCIDENT, state);
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

    private static FlowContext getFlowContext() {
        FlowContext cardContext = new FlowContext(
                "payment-approval",
                Map.of(
                        "paymentType", "credit_card",
                        "timeBucket", TimeBucketType.AFTERNOON,
                        "dayType", FlowDayType.BUSINESS_DAY
                )
        );
        return cardContext;
    }

}
