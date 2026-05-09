package org.ohrly.core.domain.entities;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Representa um evento bruto ocorrido dentro de uma jornada/fluxo digital.
 *
 * Exemplos:
 * - usuário autenticou
 * - pagamento aprovado
 * - transferido para humano
 * - etapa timeout
 */
@Data
@Builder
public class FlowEvent {

    private final String eventId;
    private final String flowId;

    /** Identifica uma execução completa da jornada.*/
    private final String sessionId;

    private final Instant occurredAt;
    private final String stepName;
    private final String status;
    private final String releaseVersion;
    private final Map<String, Object> metadata;

}