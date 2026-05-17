package repz.app.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "Ficha de treino (divisão) de um aluno")
public class TreinoResponse {

    private Long id;

    private String nome;

    private String divisao;

    private String objetivo;

    private String observacoes;

    private Boolean ativo;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate validadeAte;

    private Long alunoId;

    private String alunoNome;

    private Long personalId;

    private String personalNome;

    private Long academiaId;

    private String academiaNome;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataInclusao;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataAlteracao;

    private List<ExercicioTreinoResponse> exercicios;
}
