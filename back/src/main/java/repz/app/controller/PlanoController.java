package repz.app.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import repz.app.dto.request.PlanoPostRequest;
import repz.app.dto.request.PlanoPutRequest;
import repz.app.dto.response.PlanoResponse;

import java.util.List;

@Tag(name = "Planos", description = "Cadastro e gestão de planos")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/planos")
public interface PlanoController {

    @PostMapping
    @Operation(summary = "Criar plano", description = "Cadastra um novo plano.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    ResponseEntity<Void> criar(
            @RequestBody @Valid PlanoPostRequest dto,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping
    @Operation(summary = "Listar planos", description = "Lista os planos cadastrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planos encontrados"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    ResponseEntity<List<PlanoResponse>> findAll(
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/{id}")
    @Operation(summary = "Buscar plano", description = "Retorna os dados de um plano pelo ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    ResponseEntity<PlanoResponse> findById(
            @Parameter(description = "ID do plano", example = "1")
            @PathVariable Integer id,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar plano", description = "Atualiza os dados de um plano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    ResponseEntity<Void> atualizar(
            @Parameter(description = "ID do plano", example = "1")
            @PathVariable Integer id,
            @RequestBody @Valid PlanoPutRequest dto,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar plano", description = "Reativa um plano desativado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano ativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    ResponseEntity<Void> ativar(
            @Parameter(description = "ID do plano", example = "1")
            @PathVariable Integer id,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativar plano", description = "Desativa um plano por soft delete.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano desativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    ResponseEntity<Void> desativar(
            @Parameter(description = "ID do plano", example = "1")
            @PathVariable Integer id,
            @Parameter(description = "ID da academia no contexto da requisição", example = "1")
            @RequestHeader(value = "X-Academia-Id", required = false) Long academiaId,
            @Parameter(hidden = true) Authentication auth);
}
