package repz.app.service.treino;

import org.springframework.security.core.Authentication;
import repz.app.dto.request.TreinoCreateRequest;
import repz.app.dto.response.TreinoResponse;

import java.util.List;

public interface TreinoService {

    TreinoResponse criar(TreinoCreateRequest request, Authentication auth);

    List<TreinoResponse> obterMinhaFichaAtiva(Authentication auth);

    List<TreinoResponse> obterMeuHistorico(Authentication auth);

    List<TreinoResponse> obterFichaAtivaDoAluno(Long alunoId, Authentication auth);

    List<TreinoResponse> obterHistoricoDoAluno(Long alunoId, Authentication auth);

    TreinoResponse findById(Long id, Authentication auth);

    void ativar(Long id);

    void desativar(Long id);
}
