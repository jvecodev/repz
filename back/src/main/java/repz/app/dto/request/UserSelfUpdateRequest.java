package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para auto-edição do usuário autenticado")
public record UserSelfUpdateRequest(
        @Schema(description = "Nome do usuário", example = "Eduardo Fabri")
        @NotBlank(message = "Insira o nome.")
        String name,

        @Schema(description = "E-mail do usuário", example = "eduardo@repz.com")
        @NotBlank(message = "Insira o e-mail.")
        @Email(message = "E-mail inválido.")
        String email,

        @Schema(description = "Nova senha (opcional — só altera se preenchida)", example = "novaSenha123")
        @Size(min = 5, message = "A senha deve ter ao menos 5 caracteres.")
        String senha
) {
}
