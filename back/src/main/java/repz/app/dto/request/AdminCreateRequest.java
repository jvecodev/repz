package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação de usuário administrador")
public record AdminCreateRequest(

        @Schema(description = "Nome do administrador", example = "Admin Master")
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @Schema(description = "E-mail do administrador", example = "admin@repz.com")
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @Schema(description = "Senha com no mínimo 5 caracteres", example = "12345")
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 6, message = "A senha precisa ter no mínimo {min} caracteres.")
        String password

) {
}
