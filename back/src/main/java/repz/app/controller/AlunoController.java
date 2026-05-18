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
import org.springframework.web.bind.annotation.*;
import repz.app.dto.request.AlunoMeUpdateRequest;
import repz.app.dto.request.AlunoUpdateRequest;
import repz.app.dto.response.AlunoDetalheResponse;

import java.util.List;

@Tag(name = "Alunos", description = "Gestão de alunos")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/alunos")
public interface AlunoController {

    @GetMapping
    @Operation(summary = "Listar alunos", description = "ADMIN vê todos; GERENTE vê somente os seus; PERSONAL vê somente seus alunos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alunos encontrados"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    List<AlunoDetalheResponse> findAll(
            @Parameter(description = "ID da academia", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/me")
    @Operation(summary = "Meu perfil", description = "Retorna o perfil do aluno autenticado. Requer perfil ALUNO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil retornado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    })
    AlunoDetalheResponse obterMeuPerfil(@Parameter(hidden = true) Authentication auth);

    @PutMapping("/me")
    @Operation(summary = "Atualizar meu perfil", description = "Atualiza nome, telefone, foto e senha do aluno autenticado. Requer perfil USUARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    })
    AlunoDetalheResponse atualizarMeuPerfil(
            @Valid @RequestBody AlunoMeUpdateRequest request,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/{id}")
    @Operation(summary = "Visualizar perfil do aluno", description = "Retorna o perfil completo. PERSONAL vê somente seus alunos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aluno encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    })
    AlunoDetalheResponse findById(
            @Parameter(description = "ID do aluno", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication auth);

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar matrícula do aluno", description = "Atualiza plano e/ou personal. Requer perfil ADMIN ou GERENTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aluno atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    })
    AlunoDetalheResponse atualizar(
            @Parameter(description = "ID do aluno", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody AlunoUpdateRequest request,
            @Parameter(description = "ID da academia", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativar aluno", description = "Inativa a matrícula do aluno. Requer perfil ADMIN ou GERENTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Aluno inativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    })
    void inativar(
            @Parameter(description = "ID do aluno", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID da academia", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);
}
