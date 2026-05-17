package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para solicitar uma nova ficha de treino")
public class SolicitacaoFichaCreateRequest {

    @Schema(description = "ID do personal ao qual a solicitação será enviada", example = "3")
    private Long personalId;

    @Size(max = 500, message = "Mensagem deve ter no máximo 500 caracteres.")
    @Schema(description = "Mensagem opcional para o personal", example = "Quero foco em hipertrofia.")
    private String mensagem;
}
