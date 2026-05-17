package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados de um exercício da divisão de treino")
public class ExercicioTreinoCreateRequest {

    @Schema(description = "Nome do exercício", example = "Supino reto com barra")
    @NotBlank(message = "Nome do exercício é obrigatório.")
    private String nomeExercicio;

    @Schema(description = "Grupo muscular", example = "Peito")
    private String grupoMuscular;

    @Schema(description = "Número de séries", example = "4")
    @Positive(message = "Séries deve ser positivo.")
    private Integer series;

    @Schema(description = "Faixa de repetições", example = "8-10")
    private String repeticoes;

    @Schema(description = "Carga em quilogramas", example = "70.00")
    private BigDecimal cargaKg;

    @Schema(description = "Descanso entre séries em segundos", example = "90")
    private Integer descansoSegundos;

    @Schema(description = "Ordem do exercício na divisão", example = "1")
    private Integer ordem;

    @Schema(description = "Observação do personal para o exercício", example = "Controlar a fase excêntrica em 3 segundos.")
    private String observacao;
}
