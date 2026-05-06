package org.ohrly.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ohrly.core.domain.FlowDefinition;
import org.ohrly.core.domain.FlowStepDefinition;
import org.ohrly.core.enums.FlowStepImportance;

import java.util.List;
import java.util.Optional;

public class FlowDefinitionTest {

    @Test
    void shouldFindExistingStepByName() {
        FlowStepDefinition login = FlowStepDefinition.builder().name("login").build();
        FlowStepDefinition checkout = FlowStepDefinition.builder().name("checkout").build();
        FlowStepDefinition payment = FlowStepDefinition.builder().name("payment").build();

        FlowDefinition flowDefinition = FlowDefinition.builder()
                .id("1")
                .name("test-flow")
                .active(true)
                .steps(List.of(login, checkout, payment))
                .build();

        Assertions.assertTrue(flowDefinition.findStep("login").isPresent());
    }

    @Test
    void shouldCountHighCriticalSteps() {
        FlowStepDefinition step1 = FlowStepDefinition.builder().name("login").importance(FlowStepImportance.HIGH).build();
        FlowStepDefinition step2 = FlowStepDefinition.builder().name("checkout").importance(FlowStepImportance.LOW).build();
        FlowStepDefinition step3 = FlowStepDefinition.builder().name("payment").importance(FlowStepImportance.HIGH).build();

        FlowDefinition flowDefinition = FlowDefinition.builder()
                .id("1")
                .name("test-flow")
                .active(true)
                .steps(List.of(step1, step2, step3))
                .build();

        long result = flowDefinition.highCriticalSteps();

        Assertions.assertEquals(2, result);
    }

    @Test
    void shouldFindLastStepBasedOnOrder() {
        FlowStepDefinition step1 = FlowStepDefinition.builder().name("login").order(1).build();
        FlowStepDefinition step2 = FlowStepDefinition.builder().name("checkout").order(2).build();
        FlowStepDefinition step3 = FlowStepDefinition.builder().name("payment").order(3).build();

        FlowDefinition flowDefinition = FlowDefinition.builder()
                .steps(List.of(step1, step2, step3))
                .build();

        Optional<FlowStepDefinition> lastStep = flowDefinition.findLastStep();

        Assertions.assertTrue(lastStep.isPresent());
        Assertions.assertEquals("payment", lastStep.get().getName());
    }

}
