# REPZ — Sistema Gerenciador de Academia e Treinos Personalizados

Aplicação web para gestão de academias, com controle de acesso hierárquico por perfil,
fichas de treino personalizadas, avaliações físicas, frequência (check-in) e dashboards
gerenciais.

> Projeto da disciplina **Experiência Criativa — Projeto Final** (Sistemas de Informação).
> Equipe: Eduardo Fabri · João Vitor Correa Oliveira · Roberto Zhou.

---

## Visão geral

O sistema adota **quatro perfis** com hierarquia bem definida, onde cada nível engloba as
permissões do nível inferior:

| Nível | Perfil | Responsabilidades |
|-------|--------|-------------------|
| 1 | **ADMIN** | Controle total. Cadastra academias, usuários e outros admins. |
| 2 | **ACADEMIA** (Gerente) | Gerencia a própria unidade: personais, alunos e planos. |
| 3 | **PERSONAL** | Cria/gerencia treinos e avalia os alunos vinculados a ele. |
| 4 | **ALUNO** | Acessa treinos, faz check-in, vê avaliações e edita os próprios dados. |

---

## Arquitetura

O repositório é um monorepo com três aplicações:

```
repz/
├── back/         API REST — Spring Boot (Java 21), arquitetura em camadas
│                 Controller → Service → Repository
├── frontend/     SPA — Angular + TypeScript (PrimeNG), consumo via HttpClient
├── ai-service/   Microsserviço de IA — FastAPI (Python), geração de relatórios/chat
└── docker-compose.yaml
```

| Camada | Tecnologia | Responsabilidade |
|--------|-----------|------------------|
| Front-end | Angular + TypeScript + PrimeNG | Interface e consumo da API REST |
| Controller | Spring Boot `@RestController` | Receber requisições, validar entrada, retornar resposta |
| Service | Spring Boot `@Service` | Regras de negócio e validações de hierarquia |
| Repository | Spring Data JPA | Acesso e persistência |
| Segurança | Spring Security + JWT | Autenticação e RBAC por perfil |
| Banco de dados | **PostgreSQL** (migrations via Flyway) | Armazenamento relacional |
| Storage | MinIO (compatível com S3) | Upload de arquivos/imagens |
| IA | FastAPI + OpenRouter | Relatórios e chat assistido por IA |
| Documentação | Swagger / OpenAPI (springdoc) | Documentação interativa dos endpoints |

---

## Como executar

### Tudo via Docker Compose (recomendado)

```bash
docker compose up --build
```

Sobe PostgreSQL, MinIO, a API, o frontend e o ai-service.

| Serviço | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| API (Swagger) | http://localhost:8080/swagger-ui.html |
| Healthcheck | http://localhost:8080/actuator/health |
| MinIO Console | http://localhost:9001 |

### Rodando cada parte isolada

**Backend** (precisa de um PostgreSQL acessível):
```bash
cd back
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd frontend/repz
npm install
npm start            # http://localhost:4200
```

**AI service:**
```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload    # exige OPENROUTER_API_KEY no .env
```

---

## Usuários de teste (seed)

A migration `V999__seed_teste.sql` popula dados de demonstração. Senha de todos: `12345`.

| Perfil | E-mail |
|--------|--------|
| ADMIN | `admin@repz.com` |
| GERENTE (Academia) | `gerente@repz.com` |
| PERSONAL | `personal@repz.com` |
| ALUNO | `aluno@repz.com` |

---

## Testes e cobertura

**Backend** (JUnit + Mockito, cobertura via JaCoCo):
```bash
cd back
./mvnw test                       # relatório em target/site/jacoco/index.html
```

**Frontend** (Karma/Jasmine):
```bash
cd frontend/repz
npm test
```

---

## Documentação

A documentação de requisitos, modelagem (entidades, casos de uso, UML) e user stories está
no board do projeto: https://github.com/users/eduardofabrii/projects/6

Diagramas UML (classes, sequência e atividades) versionados em
[`docs/diagrams`](docs/diagrams/).

---

## Convenções

- **Branches:** uma por issue/feature (`<id>-descricao`), merge na `develop` ao fim da sprint.
- **Commits:** padrão *Conventional Commits* (`feat:`, `fix:`, `docs:`, `refactor:` …).
- **Migrations:** versionadas com Flyway (`Vn__descricao.sql` ou migration Java).
