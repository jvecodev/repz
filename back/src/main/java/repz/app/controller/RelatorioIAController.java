package repz.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import repz.app.dto.response.RelatorioIAResponse;

import java.util.List;

@Tag(name = "Relatórios IA", description = "Geração de relatórios de evolução física por IA")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/relatorios")
public interface RelatorioIAController {

    @PostMapping("/avaliacao/{alunoId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar geração de relatório", description = "Inicia a geração assíncrona de um relatório de evolução física via IA.")
    RelatorioIAResponse iniciar(@PathVariable Long alunoId, Authentication auth);

    @GetMapping("/{id}")
    @Operation(summary = "Consultar status do relatório")
    RelatorioIAResponse buscar(@PathVariable Long id);

    @GetMapping("/aluno/{alunoId}")
    @Operation(summary = "Listar relatórios do aluno")
    List<RelatorioIAResponse> listar(@PathVariable Long alunoId);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancelar geração do relatório")
    void cancelar(@PathVariable Long id);
}
