package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import repz.app.persistence.entity.SolicitacaoFichaStatus;

@Data
@Schema(description = "Resposta do personal a uma solicitação de ficha")
public class SolicitacaoFichaResponderRequest {

    @NotNull(message = "Status é obrigatório.")
    @Schema(description = "Novo status: APROVADA ou REJEITADA", example = "APROVADA")
    private SolicitacaoFichaStatus status;

    @Size(max = 500, message = "Resposta deve ter no máximo 500 caracteres.")
    @Schema(description = "Comentário opcional do personal", example = "Vou preparar até sexta-feira!")
    private String resposta;
}
