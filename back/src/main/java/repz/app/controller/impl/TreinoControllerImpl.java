package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.TreinoController;
import repz.app.dto.request.TreinoCreateRequest;
import repz.app.dto.response.TreinoResponse;
import repz.app.service.treino.TreinoService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TreinoControllerImpl implements TreinoController {

    private final TreinoService treinoService;

    @Override
    public TreinoResponse criar(TreinoCreateRequest request, Authentication auth) {
        return treinoService.criar(request, auth);
    }

    @Override
    public List<TreinoResponse> obterMinhaFichaAtiva(Authentication auth) {
        return treinoService.obterMinhaFichaAtiva(auth);
    }

    @Override
    public List<TreinoResponse> obterMeuHistorico(Authentication auth) {
        return treinoService.obterMeuHistorico(auth);
    }

    @Override
    public List<TreinoResponse> obterFichaAtivaDoAluno(Long aluno, Authentication auth) {
        return treinoService.obterFichaAtivaDoAluno(aluno, auth);
    }

    @Override
    public List<TreinoResponse> obterHistoricoDoAluno(Long aluno, Authentication auth) {
        return treinoService.obterHistoricoDoAluno(aluno, auth);
    }

    @Override
    public TreinoResponse findById(Long id, Authentication auth) {
        return treinoService.findById(id, auth);
    }

    @Override
    public void ativar(Long id) {
        treinoService.ativar(id);
    }

    @Override
    public void desativar(Long id) {
        treinoService.desativar(id);
    }
}
