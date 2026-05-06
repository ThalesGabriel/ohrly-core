package org.ohrly.core.domain;

import lombok.Builder;
import lombok.Data;
import org.ohrly.core.enums.FlowStepImportance;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidade central do domínio.
 *
 * Representa um fluxo digital monitorado pela Ohrly.
 *
 * Exemplos:
 * - Checkout
 * - Login
 * - Segunda via cartão
 * - Reset de senha
 * - Onboarding
 *
 * O Flow define:
 * - identidade do fluxo
 * - criticidade de negócio
 * - etapas esperadas
 * - evento final de sucesso
 * - configurações mínimas para avaliação
 */
@Data
@Builder
public class FlowDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final boolean active;

    private final List<FlowStepDefinition> steps;

    public Optional<FlowStepDefinition> findStep(String name) {
        return steps.stream()
                .filter(step -> step.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<FlowStepDefinition> findLastStep() {
        return steps.stream()
                .max(Comparator.comparingInt(FlowStepDefinition::getOrder));
    }

    public boolean containsStep(String stepName) {
        return findStep(stepName).isPresent();
    }

    public boolean hasCriticalSteps() {
        return highCriticalSteps() > 0L;
    }

    public long highCriticalSteps() {
        return steps.stream()
                .filter(step -> Objects.equals(step.getImportance(), FlowStepImportance.HIGH))
                .count();
    }

}
