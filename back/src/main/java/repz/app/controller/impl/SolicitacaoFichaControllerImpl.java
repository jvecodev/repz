package repz.app.controller.impl;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.SolicitacaoFichaController;
import repz.app.dto.request.SolicitacaoFichaCreateRequest;
import repz.app.dto.request.SolicitacaoFichaResponderRequest;
import repz.app.dto.response.SolicitacaoFichaResponse;
import repz.app.persistence.entity.SolicitacaoFichaStatus;
import repz.app.service.solicitacaoFicha.SolicitacaoFichaService;

import java.util.List;

@RestController
@AllArgsConstructor
public class SolicitacaoFichaControllerImpl implements SolicitacaoFichaController {

    private final SolicitacaoFichaService service;

    @Override
    public SolicitacaoFichaResponse criar(SolicitacaoFichaCreateRequest request, Authentication auth) {
        return service.criar(request, auth);
    }

    @Override
    public SolicitacaoFichaResponse cancelar(Long id, Authentication auth) {
        return service.cancelar(id, auth);
    }

    @Override
    public SolicitacaoFichaResponse pendente(Authentication auth) {
        return service.pendente(auth);
    }

    @Override
    public List<SolicitacaoFichaResponse> listarParaPersonal(SolicitacaoFichaStatus status, Authentication auth) {
        return service.listarParaPersonal(status, auth);
    }

    @Override
    public SolicitacaoFichaResponse responder(Long id, SolicitacaoFichaResponderRequest request, Authentication auth) {
        return service.responder(id, request, auth);
    }
}
