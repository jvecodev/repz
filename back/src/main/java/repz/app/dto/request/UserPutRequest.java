package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import repz.app.persistence.entity.UserRole;

@Schema(description = "Dados para atualização de usuário")
public record UserPutRequest(
        @Schema(description = "Nome do usuário", example = "Eduardo Fabri")
        @NotBlank(message = "Insira o nome.")
        String name,

        @Schema(description = "E-mail do usuário", example = "eduardo@repz.com")
        @NotBlank(message = "Insira o e-mail.")
        @Email(message = "E-mail inválido.")
        String email,

        @Schema(description = "Perfil do usuário", example = "ALUNO")
        UserRole role,

        @Schema(description = "Status ativo do usuário", example = "true")
        Boolean active
) {
}
