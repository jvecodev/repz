package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "Exercício de uma divisão de treino")
public class ExercicioTreinoResponse {

    private Long id;

    private String nomeExercicio;

    private String grupoMuscular;

    private Integer series;

    private String repeticoes;

    private BigDecimal cargaKg;

    private Integer descansoSegundos;

    private Integer ordem;

    private String observacao;
}
