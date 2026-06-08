package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.controller.impl.AlunoControllerImpl;
import repz.app.dto.request.AlunoMeUpdateRequest;
import repz.app.dto.request.AlunoUpdateRequest;
import repz.app.dto.response.AlunoDetalheResponse;
import repz.app.service.aluno.AlunoService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;
import static repz.app.persistence.entity.UserRole.ALUNO;

@ExtendWith(MockitoExtension.class)
class AlunoControllerUnitTest {

    @Mock private AlunoService alunoService;
    @InjectMocks private AlunoControllerImpl controller;

    @Test
    void findAllDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        var resp = new AlunoDetalheResponse();
        when(alunoService.findAll(null, auth)).thenReturn(List.of(resp));

        var result = controller.findAll(null, auth);
        assertThat(result).hasSize(1);
        verify(alunoService).findAll(null, auth);
    }

    @Test
    void obterMeuPerfilDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        var resp = new AlunoDetalheResponse();
        when(alunoService.obterMeuPerfil(auth)).thenReturn(resp);

        assertThat(controller.obterMeuPerfil(auth)).isSameAs(resp);
        verify(alunoService).obterMeuPerfil(auth);
    }

    @Test
    void atualizarMeuPerfilDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        var req = new AlunoMeUpdateRequest("Novo Nome", null, null, null);
        var resp = new AlunoDetalheResponse();
        when(alunoService.atualizarMeuPerfil(req, auth)).thenReturn(resp);

        assertThat(controller.atualizarMeuPerfil(req, auth)).isSameAs(resp);
    }

    @Test
    void findByIdDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        var resp = new AlunoDetalheResponse();
        when(alunoService.findById(1L, auth)).thenReturn(resp);

        assertThat(controller.findById(1L, auth)).isSameAs(resp);
    }

    @Test
    void atualizarDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        var req = new AlunoUpdateRequest(null, null, null);
        var resp = new AlunoDetalheResponse();
        when(alunoService.atualizar(1L, req, null, auth)).thenReturn(resp);

        assertThat(controller.atualizar(1L, req, null, auth)).isSameAs(resp);
    }

    @Test
    void inativarDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        controller.inativar(1L, null, auth);
        verify(alunoService).inativar(1L, null, auth);
    }
}
