package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.FrequenciaController;
import repz.app.dto.request.FrequenciaCreateRequest;
import repz.app.dto.response.AlunoInativoResponse;
import repz.app.dto.response.FrequenciaRelatorioResponse;
import repz.app.dto.response.FrequenciaResponse;
import repz.app.message.Mensagens;
import repz.app.service.frequencia.FrequenciaService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FrequenciaControllerImpl implements FrequenciaController {

    private final FrequenciaService frequenciaService;
    private final Mensagens mensagens;

    @Override
    public FrequenciaResponse criar(FrequenciaCreateRequest request, Long academiaId, Authentication auth) {
        return frequenciaService.criar(request, academiaId, auth);
    }

    @Override
    public List<FrequenciaResponse> findAll(
            Long aluno,
            Long academia,
            LocalDateTime inicio,
            LocalDateTime fim,
            Long academiaHeader,
            Authentication auth) {
        Long academiaId = academiaHeader != null ? academiaHeader : academia;
        if (aluno != null) {
            return frequenciaService.filtrarPorPeriodo(aluno, academiaId, inicio, fim, auth);
        } else if (academiaId != null) {
            return frequenciaService.filtrarPorAcademiaEPeriodo(academiaId, inicio, fim, auth);
        }
        throw new RuntimeException(mensagens.get("frequencia.filtro.obrigatorio"));
    }

    @Override
    public FrequenciaResponse findById(Long id) {
        return frequenciaService.findById(id);
    }

    @Override
    public List<FrequenciaResponse> meuHistorico(Authentication auth) {
        return frequenciaService.meuHistorico(auth);
    }

    @Override
    public List<AlunoInativoResponse> alunosInativos(Long academia, Long academiaHeader, Authentication auth) {
        Long academiaId = academiaHeader != null ? academiaHeader : academia;
        return frequenciaService.obterAlunosInativos(academiaId, auth);
    }

    @Override
    public FrequenciaRelatorioResponse obterRelatorio(
            Long academia,
            LocalDateTime inicio,
            LocalDateTime fim,
            Long academiaHeader,
            Authentication auth) {
        Long academiaId = academiaHeader != null ? academiaHeader : academia;
        return frequenciaService.obterRelatorio(academiaId, inicio, fim, auth);
    }

    @Override
    public void ativar(Long id) {
        frequenciaService.ativar(id);
    }

    @Override
    public void desativar(Long id) {
        frequenciaService.desativar(id);
    }

}
