package repz.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import repz.app.persistence.entity.UserRole;

@Schema(description = "Dados para criação de usuário (aluno, personal ou gerente)")
public record UserCreateRequest(

        @Schema(description = "Nome do usuário", example = "João Silva")
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @Schema(description = "E-mail do usuário", example = "joao@repz.com")
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @Schema(description = "Senha com no mínimo 5 caracteres", example = "12345")
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 5, message = "A senha precisa ter no mínimo {min} caracteres.")
        String password,

        @Schema(description = "Perfil do usuário: ALUNO, PERSONAL ou GERENTE", example = "ALUNO")
        @NotNull(message = "Perfil é obrigatório.")
        UserRole role,

        @Schema(description = "ID da academia à qual o usuário será vinculado", example = "1")
        @NotNull(message = "ID da academia é obrigatório.")
        Long academiaId,

        @Schema(description = "ID do plano (obrigatório para ALUNO, ignorado para PERSONAL e GERENTE)", example = "1")
        Integer planoId

) {
}
