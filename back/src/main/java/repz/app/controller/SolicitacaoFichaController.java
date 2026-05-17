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
import repz.app.dto.request.SolicitacaoFichaCreateRequest;
import repz.app.dto.request.SolicitacaoFichaResponderRequest;
import repz.app.dto.response.SolicitacaoFichaResponse;
import repz.app.persistence.entity.SolicitacaoFichaStatus;

import java.util.List;

@Tag(name = "Solicitações de Ficha", description = "Aluno solicita nova ficha; personal visualiza e responde")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/solicitacoes-ficha")
public interface SolicitacaoFichaController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicitar nova ficha", description = "Aluno envia uma solicitação de nova ficha ao seu personal.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação criada"),
            @ApiResponse(responseCode = "400", description = "Já existe solicitação pendente"),
            @ApiResponse(responseCode = "403", description = "Apenas alunos podem solicitar")
    })
    SolicitacaoFichaResponse criar(
            @Valid @RequestBody SolicitacaoFichaCreateRequest request,
            @Parameter(hidden = true) Authentication auth);

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar solicitação", description = "Aluno cancela uma solicitação pendente.")
    SolicitacaoFichaResponse cancelar(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/minhas")
    @Operation(summary = "Minhas solicitações", description = "Aluno lista suas próprias solicitações.")
    List<SolicitacaoFichaResponse> listarMinhas(
            @Parameter(hidden = true) Authentication auth);

    @GetMapping("/pendente")
    @Operation(summary = "Solicitação pendente", description = "Retorna a solicitação pendente do aluno, se houver (ou null).")
    SolicitacaoFichaResponse pendente(
            @Parameter(hidden = true) Authentication auth);

    @GetMapping
    @Operation(summary = "Listar para o personal", description = "Personal lista as solicitações dos seus alunos.")
    List<SolicitacaoFichaResponse> listarParaPersonal(
            @Parameter(description = "Filtro por status (PENDENTE, APROVADA, REJEITADA)")
            @RequestParam(required = false) SolicitacaoFichaStatus status,
            @Parameter(hidden = true) Authentication auth);

    @PatchMapping("/{id}/responder")
    @Operation(summary = "Responder solicitação", description = "Personal aprova ou rejeita uma solicitação.")
    SolicitacaoFichaResponse responder(
            @PathVariable Long id,
            @Valid @RequestBody SolicitacaoFichaResponderRequest request,
            @Parameter(hidden = true) Authentication auth);
}
