package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados para criação de uma divisão de ficha de treino")
public class TreinoCreateRequest {

    @Schema(description = "ID do aluno (usuário) dono da ficha", example = "1")
    @NotNull(message = "Aluno ID é obrigatório.")
    private Long alunoId;

    @Schema(description = "Nome da divisão", example = "Treino A — Peito e Tríceps")
    @NotBlank(message = "Nome do treino é obrigatório.")
    private String nome;

    @Schema(description = "Divisão (A, B, C, D)", example = "A")
    @NotBlank(message = "Divisão é obrigatória.")
    private String divisao;

    @Schema(description = "Objetivo da ficha", example = "Hipertrofia muscular com foco em membros superiores")
    private String objetivo;

    @Schema(description = "Observações gerais do personal", example = "Aquecer 8 min na esteira antes de iniciar.")
    private String observacoes;

    @Schema(description = "Data de validade da ficha", example = "2026-07-22")
    private LocalDate validadeAte;

    @Schema(description = "Exercícios da divisão")
    @Valid
    private List<ExercicioTreinoCreateRequest> exercicios;
}
