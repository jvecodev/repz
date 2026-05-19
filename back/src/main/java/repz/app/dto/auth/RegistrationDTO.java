package repz.app.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import repz.app.persistence.entity.UserRole;

@Schema(description = "Dados para cadastro de usuário")
public record RegistrationDTO(

        @Schema(description = "Nome do usuário", example = "Eduardo Fabri")
        @NotBlank(message = "Insira o nome.")
        String name,

        @Schema(description = "E-mail do usuário", example = "eduardo@repz.com")
        @NotBlank(message = "Insira o e-mail.")
        @Email(message = "E-mail inválido.")
        String email,

        @Schema(description = "Senha com no mínimo 5 caracteres", example = "12345")
        @NotBlank(message = "Insira a senha.")
        @Size(min = 6, message = "A senha precisa ter no mínimo {min} caracteres.")
        String password,

        @Schema(description = "Perfil solicitado para o usuário", example = "ALUNO")
        UserRole role

) {
}
