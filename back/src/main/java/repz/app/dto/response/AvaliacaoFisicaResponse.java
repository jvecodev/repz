package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "Dados de avaliação física")
public class AvaliacaoFisicaResponse {
    private Long id;

    private Long alunoId;

    private String alunoNome;

    private Long personalId;

    private String personalNome;

    private Long academiaId;

    private String academiaNome;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataAvaliacao;

    private Double pesoKg;

    private Double alturaCm;

    private Double imc;

    private Double percentualGordura;

    private String medidas;

    private Double cinturaCm;

    private Double quadrilCm;

    private Double bracoCm;

    private Double coxaCm;
}
