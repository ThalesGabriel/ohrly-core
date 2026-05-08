package org.ohrly.core.domain;

import lombok.Builder;
import lombok.Data;
import org.ohrly.core.enums.FlowStepImportanceType;

/**
 * Representa uma etapa esperada dentro de um fluxo.
 *
 * Ex:
 * LOGIN
 * AUTHENTICATE_USER
 * SELECT_CARD
 * GENERATE_INVOICE
 * SUCCESS
 */
@Data
@Builder
public class FlowStepDefinition {

    private String id;
    private String flowId;
    private String name;
    private int order;
    private FlowStepImportanceType importance;
    private boolean required;

    public boolean isCritical() {
        return importance == FlowStepImportanceType.HIGH;
    }
}