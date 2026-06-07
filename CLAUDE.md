# CLAUDE.md — Repz

## Stack e versões

| Camada      | Tecnologia                   | Versão      |
|-------------|------------------------------|-------------|
| Backend     | Spring Boot                  | 4.0.3       |
| Backend     | Java                         | 21          |
| Backend     | Spring Security               | 7.x (via SB4)|
| Backend     | JWT                          | Auth0 4.5.1 |
| Backend     | Flyway                       | Managed pelo SB4 parent |
| Backend     | MinIO SDK                    | 8.5.14      |
| Frontend    | Angular                      | 21.2.0      |
| Frontend    | TypeScript                   | 5.9.2       |
| Frontend    | PrimeNG                      | 21.1.3      |
| Frontend    | RxJS                         | 7.8.0       |
| Frontend    | ngx-translate                | 17.0.0      |
| AI Service  | FastAPI                      | >=0.111.0   |
| AI Service  | Python                       | 3.12        |
| AI Service  | OpenAI SDK (OpenRouter)      | >=1.30.0    |
| Banco       | PostgreSQL                   | 16          |
| Storage     | MinIO                        | latest      |

---

## Estrutura de pastas

```
repz/
├── docker-compose.yaml         # Orquestração completa (dev)
├── back/                       # Spring Boot API
│   ├── pom.xml
│   ├── Dockerfile / Dockerfile.dev
│   └── src/main/java/repz/app/
│       ├── config/             # SecurityConfig, AsyncConfig, FlywayConfig, MinioConfig, OpenApiConfig
│       ├── controller/         # Interfaces de controller (anotações Swagger aqui)
│       │   └── impl/           # Implementações (@RestController vai aqui)
│       ├── dto/
│       │   ├── auth/           # AuthenticationDTO, LoginResponseDTO, ForgotPasswordRequest, ResetPasswordRequest
│       │   ├── request/        # DTOs de entrada (records imutáveis)
│       │   └── response/       # DTOs de saída (records imutáveis)
│       ├── exception/          # GlobalExceptionHandler, ErrorResponse
│       ├── message/            # Mensagens (i18n via messages.properties)
│       ├── persistence/
│       │   ├── entity/         # Entidades JPA
│       │   │   └── common/     # AuditoriaBase (@CreationTimestamp, @UpdateTimestamp, nmUsuario)
│       │   ├── mapper/         # AcademiaMapper, UserMapper (Entity ↔ DTO)
│       │   └── repository/     # JpaRepository interfaces
│       └── service/
│           ├── academia/       # AcademiaServiceImpl
│           ├── aluno/          # AlunoServiceImpl
│           ├── avaliacaoFisica/
│           ├── email/          # EmailService
│           ├── frequencia/     # FrequenciaServiceImpl
│           ├── personal/       # PersonalServiceImpl
│           ├── plano/          # PlanoServiceImpl
│           ├── relatorio/      # RelatorioIAService, AiServiceClient
│           ├── security/       # TokenService, PasswordResetService, TokenBlacklistService
│           ├── solicitacaoFicha/
│           ├── storage/        # StorageServiceImpl (MinIO)
│           ├── treino/         # TreinoServiceImpl
│           └── user/           # UserServiceImpl
│       └── src/main/resources/
│           ├── application.properties
│           └── db/
│               ├── migration/  # V1__create_usuario.sql … V23__add_foto_url.sql (Flyway)
│               └── seed/       # V99__admin_seed.sql, V999__test_data.sql
│
├── frontend/repz/              # Angular 21 SPA + SSR
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   └── src/
│       ├── app/
│       │   ├── app.ts          # Root component standalone
│       │   ├── app.routes.ts   # Lazy-loading + guards por role
│       │   ├── app.config.ts   # provideHttpClient, PrimeNG, i18n
│       │   ├── core/
│       │   │   ├── guards/     # adminGuard, academiaGuard, alunoGuard, personalGuard
│       │   │   ├── interceptors/ # authInterceptor (JWT + refresh automático)
│       │   │   ├── services/   # auth, user, academia, aluno, personal, plano, treino,
│       │   │   │               # avaliacao-fisica, frequencia, relatorio-ia, solicitacao-ficha,
│       │   │   │               # file, theme, language, layout
│       │   │   └── validators/ # cpf-cnpj
│       │   ├── features/
│       │   │   ├── auth/       # login, forgot-password, reset-password
│       │   │   ├── admin/      # dashboard, academias, usuarios, perfil
│       │   │   ├── academia/   # dashboard, alunos, personais, planos, relatorios, perfil (role GERENTE)
│       │   │   ├── personal/   # dashboard, alunos, aluno-detalhes, ficha-treino, avaliacao-nova, perfil
│       │   │   └── aluno/      # dashboard, ficha-treino, avaliacao-fisica, frequencia, perfil
│       │   └── shared/
│       │       ├── avatar-upload/  # Componente reutilizável de upload de foto
│       │       └── layout/
│       │           ├── app-shell/  # Shell principal (header + sidebar + outlet)
│       │           ├── app-sidebar/
│       │           └── app-topbar/
│       └── environments/
│           └── environment.ts  # apiUrl: 'http://localhost:8080'
│
└── ai-service/                 # FastAPI + OpenRouter
    ├── main.py
    ├── requirements.txt
    ├── Dockerfile
    └── app/
        ├── main.py             # FastAPI app, routers: /report, /chat/stream, /health
        ├── config.py           # Settings (pydantic-settings, .env)
        ├── routers/
        │   ├── chat.py         # POST /chat/stream → SSE streaming
        │   └── report.py       # POST /report → geração de relatório buffered
        └── services/
            └── ai_service.py   # chat_stream(), report_buffer(), fallback chain
```

---

## Padrões que o projeto JÁ usa

### Backend

**Controller: sempre interface + implementação separadas**
```java
// controller/TreinoController.java — só anotações Swagger e assinatura
public interface TreinoController { ... }

// controller/impl/TreinoControllerImpl.java — @RestController aqui
@RestController @RequiredArgsConstructor
public class TreinoControllerImpl implements TreinoController { ... }
```

**DTOs: Java records imutáveis**
```java
public record AlunoCreateRequest(
    @NotNull Long userId,
    @NotNull Long academiaId,
    String objetivo
) {}
```

**Serviços: classe direta, sem interface (exceto security)**
```java
@Service @RequiredArgsConstructor @Slf4j
public class AlunoServiceImpl { ... }
```

**DI: sempre constructor injection via `@RequiredArgsConstructor`**

**Entidades: herdam `AuditoriaBase` para ter dtInclusao, dtAlteracao, nmUsuario**

**Soft delete: campo `ativo` (boolean) — não delete físico**

**Mensagens de erro: centralizadas em `Mensagens.java` + `messages.properties`**

**Repositórios: `JpaRepository` + query methods por nome**
```java
public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    Optional<Aluno> findByUsuarioId(Long usuarioId);
}
```

**Observação: `UserRepository` usa `JpaRepository<User, Integer>` apesar de `User.id` ser `Long`. Padrão do projeto usa `Math.toIntExact()` para converter Long → Integer ao buscar usuários.**

**Exceções: lançar `ResponseStatusException` ou `NoSuchElementException` — o `GlobalExceptionHandler` trata tudo.**

### Frontend

**Componentes: sempre standalone**
```typescript
@Component({ standalone: true, imports: [...], ... })
```

**Injeção de dependência: sempre `inject()` (não constructor)**
```typescript
private readonly http = inject(HttpClient);
private readonly router = inject(Router);
```

**Estado global: Signals (`signal`, `computed`)**
```typescript
readonly sessao = signal<SessaoUsuario | null>(null);
readonly autenticado = computed(() => this.sessao() !== null);
```

**Path aliases configurados em `tsconfig.json`:**
- `@core/*` → `src/app/core/*`
- `@shared/*` → `src/app/shared/*`
- `@features/*` → `src/app/features/*`
- `@env/*` → `src/environments/*`

**Serviços: `providedIn: 'root'`, nomenclatura camelCase sem sufixo (ex: `auth.ts`, `user.ts`)**

**i18n: ngx-translate com fallback `pt-BR`, arquivos em `/assets/i18n/{lang}.json`**

**Tema: PrimeNG Lara, dark mode via `[data-theme="dark"]`, toggle em `ThemeService`**

**Rotas: lazy loading para todos os features, guards por role**

### AI Service

**Fallback chain**: tenta cada modelo em sequência; falha do modelo → próximo
**Modelos free via OpenRouter** (configurável em `ai_models` no `.env`)
**Porta interna: 8000, exposta como 8055 no docker-compose**

---

## Como rodar o projeto

### Tudo junto (Docker Compose — recomendado para dev)
```bash
# Na raiz do projeto
docker compose up --build

# Serviços disponíveis:
# Frontend  → http://localhost:4200
# Backend   → http://localhost:8080
# AI Service → http://localhost:8055
# MinIO     → http://localhost:9000 (console: http://localhost:9001)
# PostgreSQL → localhost:5432
```

### Só o backend
```bash
cd back
./mvnw spring-boot:run
```

### Só o frontend
```bash
cd frontend/repz
npm install
npm start         # ng serve → http://localhost:4200
npm run build     # build de produção
npm test          # Vitest unit tests
```

### Só o AI service
```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Variáveis de ambiente necessárias

**back/.env** (mínimo):
```
JWT_SECRET=<segredo>
DB_URL=jdbc:postgresql://localhost:5432/repz
DB_USER=postgres
DB_PASSWORD=postgres
MINIO_URL=http://localhost:9000
MINIO_EXTERNAL_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=repz
AI_SERVICE_URL=http://localhost:8000
```

**ai-service/.env** (mínimo):
```
OPENROUTER_API_KEY=<chave>
```

---

## Regras ao mexer neste projeto

### Backend

1. **Não altere a estrutura de migrations existentes.** Sempre crie um novo arquivo `V{n+1}__descricao.sql`. O número V20 está faltando — a próxima migration deve ser `V24__...`.

2. **Controllers sempre têm interface.** Qualquer novo controller cria `FooController.java` (interface com Swagger) e `FooControllerImpl.java` (implementação com `@RestController`).

3. **DTOs são records.** Não use classes com getters/setters para novos DTOs.

4. **Soft delete, não hard delete.** Entidades com `ativo` nunca são deletadas do banco — apenas `ativo = false`.

5. **Nunca retorne a entidade JPA diretamente no controller.** Sempre converta para um DTO de resposta.

6. **Mensagens de erro sempre em `messages.properties`**, acessadas via `Mensagens.java`.

7. **Exceções de negócio:** use `ResponseStatusException(HttpStatus.BAD_REQUEST/NOT_FOUND/FORBIDDEN, ...)`. O `GlobalExceptionHandler` já formata a resposta.

8. **Ao adicionar endpoint, configure a autorização em `SecurityConfig`.** Use os paths exatos — typos criam regras mortas (ver AUDITORIA.md).

9. **O UserRepository usa `Integer` como chave.** Ao buscar usuário por ID Long, use `Math.toIntExact(id)` — padrão já estabelecido no projeto.

### Frontend

1. **Componentes sempre standalone.** Não use `NgModule`.

2. **Injeção sempre via `inject()`**, não via constructor.

3. **Use os path aliases** (`@core/`, `@shared/`, `@features/`, `@env/`). Nunca use caminhos relativos longos.

4. **Novos serviços:** `providedIn: 'root'`, arquivo sem sufixo Service (ex: `treino.ts`, não `treino.service.ts`).

5. **Novos guards:** padrão funcional `CanActivateFn` (não classes), na pasta `core/guards/`.

6. **Estado reativo:** prefira `signal` e `computed`. Evite BehaviorSubject para estado novo.

7. **Textos da UI:** use ngx-translate (`{{ 'chave' | translate }}`). Não hardcode strings visíveis ao usuário.

### AI Service

1. **Novos modelos** vão na variável `ai_models` do `.env`, separados por vírgula. Não altere o código.

2. **Mantenha o fallback chain.** Todo novo modelo de IA deve entrar na lista, não substituir.

3. **Não exponha o `OPENROUTER_API_KEY` em logs.**
