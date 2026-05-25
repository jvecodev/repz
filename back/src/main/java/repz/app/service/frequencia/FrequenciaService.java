package repz.app.service.frequencia;

import org.springframework.security.core.Authentication;
import repz.app.dto.request.FrequenciaCreateRequest;
import repz.app.dto.response.AlunoInativoResponse;
import repz.app.dto.response.FrequenciaRelatorioResponse;
import repz.app.dto.response.FrequenciaResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface FrequenciaService {

    FrequenciaResponse criar(FrequenciaCreateRequest request, Long academiaHeaderId, Authentication auth);

    List<FrequenciaResponse> filtrarPorPeriodo(
            Long alunoId,
            Long academiaId,
            LocalDateTime inicio,
            LocalDateTime fim,
            Authentication auth);

    List<FrequenciaResponse> filtrarPorAcademiaEPeriodo(
            Long academiaId,
            LocalDateTime inicio,
            LocalDateTime fim,
            Authentication auth);

    FrequenciaResponse findById(Long id);

    List<FrequenciaResponse> meuHistorico(Authentication auth);

    List<AlunoInativoResponse> obterAlunosInativos(Long academiaId, Authentication auth);

    FrequenciaRelatorioResponse obterRelatorio(Long academiaId, LocalDateTime inicio, LocalDateTime fim, Authentication auth);

    void ativar(Long id);

    void desativar(Long id);
}
