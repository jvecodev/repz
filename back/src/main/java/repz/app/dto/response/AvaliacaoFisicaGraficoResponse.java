package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "Dados de evolução física para gráfico")
public class AvaliacaoFisicaGraficoResponse {
    private Long alunoId;

    private String alunoNome;

    private List<DadoGrafico> dados;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Ponto de evolução física no gráfico")
    public static class DadoGrafico {
        private LocalDateTime data;
        private Double peso;
        private Double imc;
        private Double percentualGordura;
        private Double cinturaCm;
        private Double quadrilCm;
        private Double bracoCm;
        private Double coxaCm;
    }
}
