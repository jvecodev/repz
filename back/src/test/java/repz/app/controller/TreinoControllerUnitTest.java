package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.controller.impl.TreinoControllerImpl;
import repz.app.dto.request.TreinoCreateRequest;
import repz.app.dto.response.TreinoResponse;
import repz.app.service.treino.TreinoService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.persistence.entity.UserRole.PERSONAL;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class TreinoControllerUnitTest {

    @Mock private TreinoService treinoService;
    @InjectMocks private TreinoControllerImpl controller;

    private TreinoResponse treino(Long id) {
        return new TreinoResponse(id, "T", "A", null, null, true, null, 2L, "Aluno", 1L, "P", 10L, "A", null, null, List.of());
    }

    @Test
    void criarDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var req = new TreinoCreateRequest();
        req.setAlunoId(2L); req.setNome("T"); req.setDivisao("A");
        var resp = treino(1L);
        when(treinoService.criar(req, auth)).thenReturn(resp);

        assertThat(controller.criar(req, auth)).isSameAs(resp);
    }

    @Test
    void obterMinhaFichaAtivaDelegaParaServico() {
        var auth = auth(user(2L, repz.app.persistence.entity.UserRole.ALUNO));
        when(treinoService.obterMinhaFichaAtiva(auth)).thenReturn(List.of(treino(1L)));

        assertThat(controller.obterMinhaFichaAtiva(auth)).hasSize(1);
    }

    @Test
    void obterMeuHistoricoDelegaParaServico() {
        var auth = auth(user(2L, repz.app.persistence.entity.UserRole.ALUNO));
        when(treinoService.obterMeuHistorico(auth)).thenReturn(List.of());

        assertThat(controller.obterMeuHistorico(auth)).isEmpty();
    }

    @Test
    void obterFichaAtivaDoAlunoDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        when(treinoService.obterFichaAtivaDoAluno(2L, auth)).thenReturn(List.of(treino(1L)));

        assertThat(controller.obterFichaAtivaDoAluno(2L, auth)).hasSize(1);
    }

    @Test
    void obterHistoricoDoAlunoDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        when(treinoService.obterHistoricoDoAluno(2L, auth)).thenReturn(List.of());

        assertThat(controller.obterHistoricoDoAluno(2L, auth)).isEmpty();
    }

    @Test
    void findByIdDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        when(treinoService.findById(1L, auth)).thenReturn(treino(1L));

        assertThat(controller.findById(1L, auth).getId()).isEqualTo(1L);
    }

    @Test
    void ativarDelegaParaServico() {
        controller.ativar(1L);
        verify(treinoService).ativar(1L);
    }

    @Test
    void desativarDelegaParaServico() {
        controller.desativar(1L);
        verify(treinoService).desativar(1L);
    }
}
