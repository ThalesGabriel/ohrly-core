# Ohrly

Ohrly é uma plataforma para monitorar a consistência de fluxos digitais críticos após mudanças.

## 💡 Problema

Sistemas modernos possuem múltiplas camadas de validação:

- APM monitora saúde técnica
- Testes garantem cenários previstos
- BI mostra impacto agregado

Mesmo assim, fluxos críticos frequentemente quebram silenciosamente:

- etapas deixam de acontecer
- caminhos alternativos degradam
- usuários ficam presos sem erro técnico
- impacto só aparece tarde demais

## 🎯 Solução

A Ohrly interpreta o comportamento real das jornadas dos usuários e compara com a expectativa declarada do fluxo.

Isso permite detectar:

- degradações silenciosas
- etapas críticas ausentes
- inconsistências operacionais
- impacto de mudanças antes de virar incidente

## 🧠 Como funciona

```text
Flow Definition → Flow Events → Flow Session → Evaluation → Score
```

1. O cliente define um fluxo esperado (etapas, ordem, criticidade)
2. Eventos reais são enviados durante a jornada
3. A Ohrly reconstrói a sessão completa
4. O motor avalia consistência da jornada
5. Um score e insights são gerados

## 🔍 O que a Ohrly responde

- Esse fluxo está consistente?
- Qual etapa está falhando?
- Qual a gravidade?
- A jornada terminou corretamente?
- Existe comportamento inesperado?

## Estado atual do projeto

Veja detalhes em: releases/v1-core/README.md