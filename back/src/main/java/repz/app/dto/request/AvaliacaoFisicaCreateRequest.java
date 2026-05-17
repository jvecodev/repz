package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados para criação de avaliação física")
public class AvaliacaoFisicaCreateRequest {
    @Schema(description = "ID do aluno avaliado", example = "1")
    @NotNull(message = "Aluno ID é obrigatório.")
    private Long alunoId;

    @Schema(description = "Peso em quilogramas", example = "78.5")
    @NotNull(message = "Peso é obrigatório.")
    @Positive(message = "Peso deve ser positivo.")
    private Double pesoKg;

    @Schema(description = "Altura em centímetros", example = "175.0")
    @NotNull(message = "Altura é obrigatória.")
    @Positive(message = "Altura deve ser positiva.")
    private Double alturaCm;

    @Schema(description = "Percentual de gordura corporal", example = "18.5")
    private Double percentualGordura;

    @Schema(description = "Medidas corporais em texto livre (opcional)", example = "Observações gerais")
    private String medidas;

    @Schema(description = "Circunferência da cintura em cm", example = "82.0")
    @Positive(message = "Cintura deve ser positiva.")
    private Double cinturaCm;

    @Schema(description = "Circunferência do quadril em cm", example = "98.0")
    @Positive(message = "Quadril deve ser positivo.")
    private Double quadrilCm;

    @Schema(description = "Circunferência do braço em cm", example = "36.0")
    @Positive(message = "Braço deve ser positivo.")
    private Double bracoCm;

    @Schema(description = "Circunferência da coxa em cm", example = "58.0")
    @Positive(message = "Coxa deve ser positiva.")
    private Double coxaCm;
}
