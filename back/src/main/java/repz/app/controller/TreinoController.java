package repz.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import repz.app.dto.request.TreinoCreateRequest;
import repz.app.dto.response.TreinoResponse;

import java.util.List;

@Tag(name = "Treinos", description = "Fichas de treino e suas divisões (A/B/C/D)")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/treinos")
public interface TreinoController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar divisão de ficha", description = "Cria uma divisão de treino para um aluno. Requer perfil PERSONAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Divisão criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    TreinoResponse criar(
            @Valid @RequestBody TreinoCreateRequest request,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/me")
    @Operation(summary = "Minha ficha ativa", description = "Lista as divisões da ficha de treino ativa do aluno autenticado. Requer perfil USUARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha ativa retornada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    List<TreinoResponse> obterMinhaFichaAtiva(@Parameter(hidden = true) Authentication auth);

    @GetMapping("/me/historico")
    @Operation(summary = "Meu histórico de fichas", description = "Lista as fichas de treino anteriores (inativas) do aluno autenticado. Requer perfil USUARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    List<TreinoResponse> obterMeuHistorico(@Parameter(hidden = true) Authentication auth);

    @GetMapping
    @Operation(summary = "Ficha ativa do aluno", description = "Lista as divisões da ficha ativa de um aluno. PERSONAL vê seus alunos; GERENTE/ADMIN também.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha ativa retornada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    List<TreinoResponse> obterFichaAtivaDoAluno(
            @Parameter(description = "ID do aluno", example = "1")
            @RequestParam Long aluno,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/historico")
    @Operation(summary = "Histórico de fichas do aluno", description = "Lista as fichas anteriores (inativas) de um aluno.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    List<TreinoResponse> obterHistoricoDoAluno(
            @Parameter(description = "ID do aluno", example = "1")
            @RequestParam Long aluno,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/{id}")
    @Operation(summary = "Buscar divisão", description = "Retorna uma divisão de treino pelo ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Divisão encontrada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Divisão não encontrada")
    })
    TreinoResponse findById(
            @Parameter(description = "ID da divisão de treino", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication auth);

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar divisão", description = "Reativa uma divisão de treino. Requer perfil PERSONAL ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Divisão ativada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Divisão não encontrada")
    })
    void ativar(
            @Parameter(description = "ID da divisão de treino", example = "1")
            @PathVariable Long id);

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativar divisão", description = "Desativa (encerra) uma divisão de treino por soft delete. Requer perfil PERSONAL ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Divisão desativada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Divisão não encontrada")
    })
    void desativar(
            @Parameter(description = "ID da divisão de treino", example = "1")
            @PathVariable Long id);
}
