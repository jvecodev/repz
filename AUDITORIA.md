# AUDITORIA.md — Repz

> Gerado em 2026-06-07. Problemas reais encontrados por leitura direta dos arquivos.
> Escolhas consistentes do projeto não são reportadas como erro.

---

## 🔴 Crítico (pode causar bug em produção)

---

### 1. SecurityConfig — regras de segurança mortas por typo

- **Arquivo:** `back/src/main/java/repz/app/config/SecurityConfig.java` linhas 54–55 e 61
- **Problema:** Cinco URLs estão com typo — falta a `/` separando o caminho do sufixo. Elas nunca vão corresponder a nenhuma requisição real, tornando as regras inativas. Os endpoints reais caem em `.anyRequest().authenticated()`, que permite qualquer usuário autenticado (não só ADMIN/GERENTE/PERSONAL como pretendido).

```java
// Linha 54-55 — regras mortas:
.requestMatchers(
    "/v3/api-docsativar",    // nunca bate em nada
    "/api/usersativar",      // deveria ser /api/users/ativar ou /api/users/inativar
    "/api/academiasinativar" // deveria ser /api/academias/inativar
).hasAnyRole("ADMIN", "GERENTE")

// Linha 61 — regras mortas (e duplicata):
.requestMatchers(HttpMethod.GET,
    "/api/checkinsativar",  // duplicado + typo
    "/api/checkinsativar",  // duplicado
    "/api/avaliacoesativar",// typo — /api/avaliacoes-fisica/ativar ?
    "/api/treinos/*/desativar"
).hasAnyRole("PERSONAL", "ADMIN")
```

- **Sugestão:** Levantar quais endpoints de ativar/desativar existem de fato nos controllers e reescrever essas regras com os paths corretos. Remover a entrada duplicada de `/api/checkinsativar`.

---

### 2. Token de reset de senha logado em INFO (vaza em logs de produção)

- **Arquivo:** `back/src/main/java/repz/app/service/security/PasswordResetService.java` linha 51
- **Problema:** O token de 6 dígitos aparece explicitamente no log em nível INFO. Em qualquer ambiente com coleta centralizada de logs (Datadog, CloudWatch, ELK) isso expõe o token a quem tem acesso aos logs, contornando toda a segurança do fluxo de reset.

```java
log.info("[PasswordReset] Token gerado para {}: {}", user.getEmail(), token.getToken());
```

- **Sugestão:** Rebaixar para `log.debug` (desativado em produção) ou remover. Manter apenas o log do email sem o token.

```java
log.debug("[PasswordReset] Token gerado para {}", user.getEmail());
```

---

### 3. Token de reset com entropia insuficiente (brute-forceable)

- **Arquivo:** `back/src/main/java/repz/app/service/security/PasswordResetService.java` linha 46
- **Problema:** O token é gerado como `String.format("%06d", SECURE_RANDOM.nextInt(1_000_000))` — apenas 1 milhão de combinações. Sem rate limiting no endpoint `/api/auth/reset-password`, um atacante pode tentar todos os tokens em segundos. O TTL de 30 minutos amplifica a janela de ataque.
- **Sugestão:** Usar token de maior entropia (ex: UUID ou 32 bytes hex via `SecureRandom`), ou manter o 6 dígitos mas adicionar rate limiting e lock após X tentativas erradas.

```java
// Alternativa com UUID:
token.setToken(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
```

---

### 4. `UserRepository` declara chave primária como `Integer`, mas `User.id` é `Long`

- **Arquivo:** `back/src/main/java/repz/app/persistence/repository/UserRepository.java` linha 11
- **Problema:** `UserRepository extends JpaRepository<User, Integer>` mas a entidade tem `@Id private Long id`. Isso força `Math.toIntExact()` em 7+ lugares nos serviços. Se um ID ultrapassar `Integer.MAX_VALUE` (2.147.483.647), todos esses pontos lançam `ArithmeticException` em runtime. Na prática improvável, mas é arquitetura incorreta que pode causar bugs difíceis de rastrear.

```java
// Atual:
public interface UserRepository extends JpaRepository<User, Integer>

// Correto:
public interface UserRepository extends JpaRepository<User, Long>
```

- **Sugestão:** Mudar para `Long` e remover todos os `Math.toIntExact()` nos serviços. Verificar se `findByIdAndDeletedAtIsNull(Integer id)` também precisa ser atualizado para `Long`.

---

## 🟡 Médio (degradação de qualidade ou manutenção difícil)

---

### 5. Timezone UTC-3 hardcoded na expiração do JWT

- **Arquivo:** `back/src/main/java/repz/app/service/security/TokenService.java` linhas 36 e 49
- **Problema:** A expiração do token é calculada com `ZoneOffset.of("-03:00")` fixo. Se o servidor for rodado em qualquer outra timezone (AWS us-east-1, Europa, etc.), o token expira na hora errada — 1h de acesso pode virar menos ou mais.

```java
.withExpiresAt(LocalDateTime.now().plusMinutes(60).toInstant(ZoneOffset.of("-03:00")))
```

- **Sugestão:** Usar `Instant` diretamente, que é sempre UTC:

```java
.withExpiresAt(Instant.now().plusSeconds(3600))
```

---

### 6. Sem `environment.prod.ts` — produção aponta para `localhost:8080`

- **Arquivo:** `frontend/repz/src/environments/environment.ts`
- **Problema:** Existe apenas um arquivo de ambiente com `production: false` e `apiUrl: 'http://localhost:8080'`. Não há `environment.prod.ts` nem configuração de `fileReplacements` no `angular.json` para produção. Um build com `--configuration production` vai continuar usando `localhost:8080`.
- **Sugestão:** Criar `environment.prod.ts` e configurar `fileReplacements` no `angular.json`:

```typescript
// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.repz.com.br',
};
```

---

### 7. CORS hardcoded apenas para `localhost` (vai falhar em produção)

- **Arquivo:** `back/src/main/java/repz/app/config/SecurityConfig.java` linhas 115–118
- **Problema:** `setAllowedOrigins` lista apenas `http://localhost:4200` e `http://127.0.0.1:4200`. Qualquer deploy em domínio real vai gerar erros CORS bloqueando o frontend inteiramente.
- **Sugestão:** Ler os origins permitidos de uma propriedade configurável:

```java
@Value("${cors.allowed-origins:http://localhost:4200}")
private List<String> allowedOrigins;
// ...
config.setAllowedOrigins(allowedOrigins);
```

---

### 8. Self-injection via `@Autowired @Lazy` em `RelatorioIAService`

- **Arquivo:** `back/src/main/java/repz/app/service/relatorio/RelatorioIAService.java` linha 36–37
- **Problema:** O workaround de self-injection é necessário para que `@Async` funcione, mas é frágil: quebra com AOT compilation, confunde ferramentas de análise estática, e pode causar ciclos de dependência inesperados se a classe for refatorada.

```java
@Autowired @Lazy
private RelatorioIAService self;
```

- **Sugestão:** Extrair o método `gerarAsync` para uma classe separada (ex: `RelatorioIAAsyncWorker`) que injeta os repositórios diretamente. Elimina o workaround completamente.

---

### 9. `logout()` no frontend faz a mesma limpeza duas vezes

- **Arquivo:** `frontend/repz/src/app/core/services/auth.ts` linhas 56–68
- **Problema:** `limparSessao()` já remove os três itens do `localStorage` e chama `this.sessao.set(null)`. Logo depois, o método `logout()` remove os mesmos itens e chama `sessao.set(null)` novamente. Sem impacto funcional, mas indica que uma das duas limpezas está desatualizada.

```typescript
logout(): void {
    this.limparSessao();               // remove TOKEN_KEY, REFRESH_KEY, ROLE_KEY, sessao.set(null)
    localStorage.removeItem(TOKEN_KEY); // redundante
    localStorage.removeItem(REFRESH_KEY); // redundante
    localStorage.removeItem(ROLE_KEY);    // redundante
    this.sessao.set(null);             // redundante
    this.userService.resetar();
}
```

- **Sugestão:** Mover `this.userService.resetar()` para dentro de `limparSessao()` ou remover as linhas redundantes do `logout()`.

---

### 10. Sem paginação nos endpoints de listagem

- **Arquivo:** Múltiplos services — `AlunoServiceImpl`, `PersonalServiceImpl`, `TreinoServiceImpl`, etc.
- **Problema:** Todos os `findAll()` retornam a lista inteira sem `Pageable`. Com volume crescente de dados (academias com centenas de alunos, treinos, avaliações), as respostas vão crescer indefinidamente e comprometer performance e memória.
- **Sugestão:** Adicionar `Pageable` aos endpoints de listagem críticos. Prioridade para: alunos por academia, treinos por aluno, frequências.

---

### 11. Migration V20 está faltando

- **Arquivo:** `back/src/main/resources/db/migration/`
- **Problema:** As migrations vão de V19 direto para V21, pulando V20. Isso pode causar conflito se alguém tentar criar uma migration V20 no futuro (Flyway vai rejeitar por checksum de versão fora de ordem com `outOfOrder=false`).
- **Sugestão:** Documentar que V20 foi intencionalmente pulada (comentário no V21), ou garantir que `outOfOrder=true` está configurado. A próxima migration deve ser `V24__...`.

---

### 12. Pastas `features/alunos` e `features/personais` parecem resquícios

- **Arquivo:** `frontend/repz/src/app/features/alunos/` e `frontend/repz/src/app/features/personais/`
- **Problema:** Existem duas pastas sem prefixo de role (`alunos`, `personais`) além das corretas dentro de `academia/` e `personal/`. Pode ser código morto de uma refatoração anterior.
- **Sugestão:** Verificar se há componentes ativos nessas pastas. Se estiverem vazias ou sem uso, removê-las para não confundir a estrutura.

---

## 🟢 Sugestão (padronização, legibilidade, boas práticas)

---

### 13. Decodificação manual do JWT no frontend

- **Arquivo:** `frontend/repz/src/app/core/services/auth.ts` linhas 115–133
- **Problema:** O payload do JWT é decodificado manualmente via `atob` + `split`. A lógica é correta (SPAs não validam assinatura — isso é responsabilidade do servidor), mas é verbosa e pode falhar silenciosamente em edge cases.
- **Sugestão:** Nenhuma ação urgente necessária. Se quiser simplificar no futuro, considere a biblioteca `jwt-decode` (0.9KB minificado). Manter o código atual é aceitável.

---

### 14. `initTokenExpiry` — não há renovação proativa de token

- **Arquivo:** `frontend/repz/src/app/core/interceptors/` (authInterceptor)
- **Problema:** O token é renovado apenas *após* receber um 401. Se o usuário está em uma operação longa (ex: formulário de treino) e o token expira, a requisição falha, é refeita com o novo token — geralmente transparente, mas pode causar race conditions em requisições paralelas.
- **Sugestão:** Sugestão apenas. O comportamento atual é aceitável para a maioria dos casos.

---

### 15. Sem rate limiting em endpoints sensíveis

- **Arquivo:** `back/src/main/java/repz/app/config/SecurityConfig.java` (falta configuração)
- **Problema:** `/api/auth/login` e `/api/auth/forgot-password` não têm rate limiting. Um atacante pode tentar senhas infinitamente (brute force) ou abusar do endpoint de forgot-password para spammar e-mails.
- **Sugestão:** Adicionar `bucket4j` + `spring-boot-starter-cache` para rate limiting por IP. Prioridade para `forgot-password` (problema 3 acima torna isso mais urgente).

---

### 16. `gerarAsync` em `RelatorioIAService` busca o relatório duas vezes

- **Arquivo:** `back/src/main/java/repz/app/service/relatorio/RelatorioIAService.java` linhas 111 e 122
- **Problema:** O relatório é buscado do banco antes de chamar a IA (linha 111) e novamente depois (linha 122), causando duas queries desnecessárias. A segunda busca existe para "pegar a versão mais recente", mas como o método é `@Async` e o relatório não muda entre as duas chamadas, uma única busca é suficiente.
- **Sugestão:** Reutilizar o objeto `relatorio` da primeira busca nas atualizações finais.

---

### 17. `application.properties` com `spring.jpa.show-sql=true` ativo por padrão

- **Arquivo:** `back/src/main/resources/application.properties`
- **Problema:** `show-sql=true` imprime cada query SQL no log, o que é útil em desenvolvimento mas gera volume enorme de log em produção e pode expor dados sensíveis.
- **Sugestão:** Mover para `application-dev.properties` e desativar no perfil de produção.

---

*Total: 6 críticos → 2 críticos reais (1: regras mortas de segurança, 2: token em log), 1 crítico de segurança menor (3: entropia do token), 1 arquitetural (4: tipo de ID). Restante são melhorias de qualidade.*
