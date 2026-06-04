package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "Relatório de check-ins por período")
public class FrequenciaRelatorioResponse {
    private Long academiaId;

    private Map<String, LocalDateTime> periodo;

    private Long totalFrequencias;

    private Map<String, Long> frequenciaPorAluno;

    @Schema(description = "Check-ins por hora do dia (chave: \"0\"–\"23\")")
    private Map<String, Long> ocupacaoPorHora;

    @Schema(description = "Check-ins por mês em ordem cronológica (chave: \"yyyy-MM\")")
    private Map<String, Long> frequenciaPorMes;
}
