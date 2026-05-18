package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import repz.app.persistence.entity.UserRole;

import java.time.LocalDateTime;

@Schema(description = "Dados de usuário")
public record UserGetResponse(
        @Schema(description = "ID do usuário", example = "1")
        Long id,

        @Schema(description = "Nome do usuário", example = "Eduardo Fabri")
        String name,

        @Schema(description = "E-mail do usuário", example = "eduardo@repz.com")
        String email,

        @Schema(description = "Data e hora do último login")
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime lastLogin,

        @Schema(description = "Perfil do usuário", example = "ALUNO")
        UserRole role,

        @Schema(description = "Status ativo do usuário", example = "true")
        Boolean active
) {}
