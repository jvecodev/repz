package repz.app.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para redefinição de senha")
public record ResetPasswordRequest(

        @Schema(description = "Código recebido por e-mail")
        @NotBlank(message = "Código inválido.")
        String token,

        @Schema(description = "Nova senha com no mínimo 5 caracteres", example = "novaSenha123")
        @NotBlank(message = "Insira a nova senha.")
        @Size(min = 5, message = "A senha precisa ter no mínimo {min} caracteres.")
        String newPassword
) {
}
