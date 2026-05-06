# Ohrly v1 — Core Consistency Engine

Esta versão representa o núcleo da Ohrly: o motor de avaliação de consistência de fluxos.

## 🎯 Objetivo

Provar que é possível:

- Reconstruir jornadas reais
- Comparar com expectativa do fluxo
- Detectar inconsistências
- Gerar um score objetivo

## 🧱 Componentes principais

### FlowDefinition

Define o fluxo esperado:

- etapas
- ordem
- criticidade
- obrigatoriedade
- ativação de steps

### FlowSession

Representa uma jornada real do usuário:

- eventos recebidos
- estado da sessão (aberta/fechada)
- motivo de término
- eventos tardios (late events)

### FlowService

Responsável por interpretar a sessão:

Gera findings como:

- etapa obrigatória ausente
- etapa final não atingida
- timeout
- handoff humano
- eventos após fechamento

### ConsistencyScorerService

Transforma findings em score:

- 0 a 100
- HEALTHY / ATTENTION / DEGRADED

## 📊 KPIs suportados

Nesta versão, é possível medir:

- Flow Consistency Score (por sessão)
- Success vs Failure rate
- Timeout rate
- Human handoff rate
- Missing critical steps
- Final step completion
- Late event rate

## 🔍 Exemplo de avaliação

```text
Flow: Second Card Copy

Session result:
- Missing required step: identify_card
- Ended with SUCCESS

Score:
70 (ATTENTION)