package repz.app.service.solicitacaoFicha;

import org.springframework.security.core.Authentication;
import repz.app.dto.request.SolicitacaoFichaCreateRequest;
import repz.app.dto.request.SolicitacaoFichaResponderRequest;
import repz.app.dto.response.SolicitacaoFichaResponse;
import repz.app.persistence.entity.SolicitacaoFichaStatus;

import java.util.List;

public interface SolicitacaoFichaService {

    /** Aluno envia uma solicitação de nova ficha para o seu personal. */
    SolicitacaoFichaResponse criar(SolicitacaoFichaCreateRequest request, Authentication auth);

    /** Aluno cancela uma solicitação pendente própria. */
    SolicitacaoFichaResponse cancelar(Long id, Authentication auth);

    /** Aluno consulta se tem solicitação pendente (atalho). */
    SolicitacaoFichaResponse pendente(Authentication auth);

    /** Personal lista as solicitações dos seus alunos, opcionalmente filtrando por status. */
    List<SolicitacaoFichaResponse> listarParaPersonal(SolicitacaoFichaStatus status, Authentication auth);

    /** Personal responde (aprova ou rejeita) uma solicitação. */
    SolicitacaoFichaResponse responder(Long id, SolicitacaoFichaResponderRequest request, Authentication auth);
}
