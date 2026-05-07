# Ohrly v2 — Behavioral Degradation Engine

Esta versão representa a evolução da Ohrly de um motor de consistência para um motor de interpretação comportamental de fluxos.

O foco deixa de ser apenas:

* “o fluxo funcionou?”

e passa a ser:

* “o fluxo continua se comportando como deveria?”

---

# 🎯 Objetivo

Provar que é possível:

* identificar degradações silenciosas
* contextualizar comportamento esperado
* diferenciar ruído de risco real
* detectar padrões que antecedem incidentes
* medir impacto operacional potencial
* gerar sinais acionáveis antes do incidente

---

# 🧠 Tese central

Sistemas raramente quebram instantaneamente.

Na maioria dos casos eles:

* degradam gradualmente
* acumulam fricção
* aumentam variabilidade
* perdem eficiência operacional
* reduzem conversão silenciosamente

A Ohrly interpreta esses sinais antes que o incidente seja oficialmente declarado.

---

# 🧱 Componentes principais

## ContextClassifier

Responsável por contextualizar eventos operacionais.

Atualmente considera:

* tipo de pagamento
* período do dia
* dia útil vs fim de semana

Exemplo:

```text
credit_card-AFTERNOON-BUSINESS_DAY
```

Objetivo:

* evitar comparações injustas
* criar expectativa comportamental contextual

---

## ApprovalEventBuilderService

Reconstrói eventos comportamentais reais a partir dos datasets.

Atualmente:

* purchase timestamp
* approval timestamp
* payment type

Gera:

* ApprovalEvent

Com:

* tempo de aprovação
* contexto operacional

---

## BaselineService

Calcula o comportamento esperado por contexto.

Atualmente:

* mediana
* p95

Objetivo:

* criar expectativa saudável contextual
* reduzir influência de extremos
* evitar falsos positivos

---

## DailyAggregationService

Agrupa eventos por:

* contexto
* dia

Produz:

* médias diárias
* volume
* valor transacionado

Objetivo:

* transformar eventos em comportamento observável

---

## BehaviorAnalyzer

Interpreta degradação ativa.

Estados suportados:

* NORMAL
* ATTENTION
* SUSTAINED_DEGRADATION
* PRE_INCIDENT

A análise considera:

* baseline contextual
* persistência
* consecutividade
* sensibilidade do fluxo
* multiplicadores configuráveis

---

## FlowBehaviorPolicy

Permite comportamento específico por fluxo.

Cada fluxo pode possuir:

* sensibilidade própria
* thresholds próprios
* lookback próprio
* métricas críticas próprias
* tolerância diferente a ruído

Exemplos:

* checkout → agressivo
* chatbot → conservador
* onboarding → balanceado

---

## BehaviorPatternExtractor

Extrai padrões históricos anteriores a degradações críticas.

Objetivo:

* aprender comportamento pré-incidente
* identificar precedência comportamental

---

## BehaviorPrecedenceService

Compara o comportamento atual com padrões históricos críticos.

Objetivo:

* detectar quando o sistema entra em comportamento semelhante a episódios anteriores de degradação

Exemplo:

```text
“O padrão atual possui 0.81 de similaridade com episódios históricos críticos.”
```

---

## BehaviorImpactCalculatorService

Calcula impacto operacional potencial.

Métricas atuais:

* duração da degradação
* pedidos impactados
* excesso acumulado de tempo
* valor financeiro potencialmente impactado

Objetivo:

* transformar comportamento em impacto compreensível

---

## BehaviorKpiService

Orquestrador principal da análise.

Responsável por:

* interpretar estado atual
* calcular impacto
* calcular precedência
* gerar mensagem final
* consolidar KPI operacional

---

# 📊 KPIs suportados

Nesta versão, é possível medir:

## KPIs comportamentais

* Behavior State
* Deviation Ratio
* Sustained Degradation Duration
* Pre-Incident Detection
* Behavioral Similarity Score
* Flow Sensitivity Level

---

## KPIs operacionais

* Impacted Orders
* Excess Approval Minutes
* Potentially Impacted Payment Value
* Degradation Persistence

---

## KPIs estratégicos

* Silent Degradation Detection
* Precedence Match Rate
* Recovery Opportunity
* Operational Risk Exposure

---

# 🔍 Exemplo de insight

```text
Context: credit_card-AFTERNOON-WEEKEND

Expected behavior:
15 min

Current behavior:
75 min

Deviation:
5.01x above expected

Persistence:
2 consecutive periods

Impact:
46 impacted orders
4226 excess minutes

Precedence:
Current pattern matches previous critical degradation episodes.

State:
PRE_INCIDENT
```

---

# 🚀 Evolução em relação à v1

## v1

A Ohrly avaliava:

* consistência estrutural do fluxo

Pergunta central:

```text
“O fluxo foi executado corretamente?”
```

---

## v2

A Ohrly avalia:

* consistência comportamental contínua

Pergunta central:

```text
“O fluxo continua saudável após mudanças?”
```

---

# 🧠 Diferencial da v2

A Ohrly deixa de ser apenas:

* análise de jornada

e passa a ser:

* interpretação operacional contínua

A plataforma agora consegue:

* contextualizar comportamento
* identificar degradação silenciosa
* detectar precedência de incidente
* transformar comportamento em decisão

---

# 🎯 Resultado esperado

Permitir que empresas:

* ajam antes do incidente
* reduzam retrabalho operacional
* diminuam perda invisível de conversão
* entendam impacto real de mudanças
* tomem decisões com mais confiança

---

# 🔮 Próximos passos previstos (v3)

* múltiplas métricas por fluxo
* retry/failure/conversion analysis
* detecção automática de recovery
* causalidade operacional
* recommendation engine
* score de confiança da mudança
* benchmark entre fluxos/squads
    