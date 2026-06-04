# Diagramas (UML)

Modelagem do REPZ em PlantUML. Para visualizar: cole o conteúdo em
[plantuml.com](https://www.plantuml.com/plantuml) ou use a extensão **PlantUML** no
VS Code (Alt+D para preview).

| Arquivo | Tipo | O que documenta |
|---------|------|-----------------|
| [`class-diagram.puml`](class-diagram.puml) | Classes | Entidades de domínio e relacionamentos |
| [`seq-autenticacao.puml`](seq-autenticacao.puml) | Sequência | Login JWT, refresh de token e logout |
| [`seq-criar-ficha-treino.puml`](seq-criar-ficha-treino.puml) | Sequência | Personal cria e atribui ficha de treino |
| [`atividade-checkin.puml`](atividade-checkin.puml) | Atividades | Fluxo de check-in (frequência) do aluno |

> Gerar PNG/SVG via CLI: `plantuml docs/diagrams/*.puml`
