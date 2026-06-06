package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "Dados de personal")
public class PersonalResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String email;
    private Long academiaId;
    private String academiaNome;
    private String especialidade;
    private Boolean ativo;
    private String fotoUrl;
}
