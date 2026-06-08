package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.controller.impl.SolicitacaoFichaControllerImpl;
import repz.app.dto.request.SolicitacaoFichaCreateRequest;
import repz.app.dto.request.SolicitacaoFichaResponderRequest;
import repz.app.dto.response.SolicitacaoFichaResponse;
import repz.app.persistence.entity.SolicitacaoFichaStatus;
import repz.app.service.solicitacaoFicha.SolicitacaoFichaService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static repz.app.persistence.entity.UserRole.ALUNO;
import static repz.app.persistence.entity.UserRole.PERSONAL;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class SolicitacaoFichaControllerUnitTest {

    @Mock private SolicitacaoFichaService service;
    @InjectMocks private SolicitacaoFichaControllerImpl controller;

    private SolicitacaoFichaResponse sol(Long id, SolicitacaoFichaStatus status) {
        return new SolicitacaoFichaResponse(id, 10L, "Aluno", 5L, "Personal", "msg", status, null, null, null);
    }

    @Test
    void criarDelegaParaServico() {
        var auth = auth(user(10L, ALUNO));
        var req = new SolicitacaoFichaCreateRequest();
        var resp = sol(1L, SolicitacaoFichaStatus.PENDENTE);
        when(service.criar(req, auth)).thenReturn(resp);

        assertThat(controller.criar(req, auth)).isSameAs(resp);
    }

    @Test
    void cancelarDelegaParaServico() {
        var auth = auth(user(10L, ALUNO));
        var resp = sol(1L, SolicitacaoFichaStatus.CANCELADA);
        when(service.cancelar(1L, auth)).thenReturn(resp);

        assertThat(controller.cancelar(1L, auth).getStatus()).isEqualTo(SolicitacaoFichaStatus.CANCELADA);
    }

    @Test
    void pendenteDelegaParaServico() {
        var auth = auth(user(10L, ALUNO));
        var resp = sol(1L, SolicitacaoFichaStatus.PENDENTE);
        when(service.pendente(auth)).thenReturn(resp);

        assertThat(controller.pendente(auth)).isSameAs(resp);
    }

    @Test
    void listarParaPersonalDelegaParaServico() {
        var auth = auth(user(5L, PERSONAL));
        var resp = List.of(sol(1L, SolicitacaoFichaStatus.PENDENTE));
        when(service.listarParaPersonal(SolicitacaoFichaStatus.PENDENTE, auth)).thenReturn(resp);

        assertThat(controller.listarParaPersonal(SolicitacaoFichaStatus.PENDENTE, auth)).hasSize(1);
    }

    @Test
    void responderDelegaParaServico() {
        var auth = auth(user(5L, PERSONAL));
        var req = new SolicitacaoFichaResponderRequest();
        req.setStatus(SolicitacaoFichaStatus.APROVADA);
        var resp = sol(1L, SolicitacaoFichaStatus.APROVADA);
        when(service.responder(1L, req, auth)).thenReturn(resp);

        assertThat(controller.responder(1L, req, auth).getStatus()).isEqualTo(SolicitacaoFichaStatus.APROVADA);
    }
}
