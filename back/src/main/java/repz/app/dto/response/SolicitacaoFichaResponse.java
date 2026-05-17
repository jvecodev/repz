package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import repz.app.persistence.entity.SolicitacaoFichaStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "Solicitação de nova ficha de treino")
public class SolicitacaoFichaResponse {

    private Long id;

    private Long alunoId;
    private String alunoNome;

    private Long personalId;
    private String personalNome;

    private String mensagem;

    private SolicitacaoFichaStatus status;

    private String resposta;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime criadaEm;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime respondidaEm;
}
