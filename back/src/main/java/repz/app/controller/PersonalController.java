package repz.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import repz.app.dto.request.PersonalUpdateRequest;
import repz.app.dto.response.PersonalAlunosResponse;
import repz.app.dto.response.PersonalResponse;

import java.util.List;

@Tag(name = "Personais", description = "Gestão de personais")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/personais")
public interface PersonalController {

    @GetMapping
    @Operation(summary = "Listar personais", description = "Lista os personais disponíveis para o perfil autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Personais encontrados"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    List<PersonalResponse> findAll(
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/{id}")
    @Operation(summary = "Buscar personal", description = "Retorna os dados de um personal pelo ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Personal encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Personal não encontrado")
    })
    PersonalResponse findById(
            @Parameter(description = "ID do personal", example = "1")
            @PathVariable Long id);

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar personal", description = "Atualiza os dados de um personal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Personal atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Personal não encontrado")
    })
    PersonalResponse atualizar(
            @Parameter(description = "ID do personal", example = "1") @PathVariable Long id,
            @Valid @RequestBody PersonalUpdateRequest request,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar personal", description = "Reativa um personal desativado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Personal ativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Personal não encontrado")
    })
    PersonalResponse ativar(
            @Parameter(description = "ID do personal", example = "1") @PathVariable Long id,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativar personal", description = "Desativa um personal por soft delete.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Personal desativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Personal não encontrado")
    })
    PersonalResponse desativar(
            @Parameter(description = "ID do personal", example = "1") @PathVariable Long id,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/me")
    @Operation(summary = "Meu perfil", description = "Retorna os dados do personal autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    PersonalResponse obterMeuPerfil(@Parameter(hidden = true) Authentication auth);

    @PutMapping("/me")
    @Operation(summary = "Atualizar meu perfil", description = "Atualiza os dados do personal autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    PersonalResponse atualizarMeuPerfil(
            @Valid @RequestBody PersonalUpdateRequest request,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/me/alunos")
    @Operation(summary = "Listar meus alunos", description = "Lista os alunos vinculados ao personal autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alunos encontrados"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    PersonalAlunosResponse obterMeusAlunos(@Parameter(hidden = true) Authentication auth);

}
