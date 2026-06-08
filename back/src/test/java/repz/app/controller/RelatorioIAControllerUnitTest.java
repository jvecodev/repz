package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.controller.impl.RelatorioIAControllerImpl;
import repz.app.dto.request.RelatorioIAUpdateRequest;
import repz.app.dto.response.RelatorioIAResponse;
import repz.app.persistence.entity.RelatorioStatus;
import repz.app.service.relatorio.RelatorioIAService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.persistence.entity.UserRole.ALUNO;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class RelatorioIAControllerUnitTest {

    @Mock private RelatorioIAService relatorioIAService;
    @InjectMocks private RelatorioIAControllerImpl controller;

    private RelatorioIAResponse rel(Long id) {
        return new RelatorioIAResponse(id, RelatorioStatus.PENDENTE, null, null);
    }

    @Test
    void iniciarDelegaParaServico() {
        var auth = auth(user(10L, ALUNO));
        when(relatorioIAService.iniciar(10L, auth)).thenReturn(rel(1L));

        assertThat(controller.iniciar(10L, auth).getId()).isEqualTo(1L);
    }

    @Test
    void buscarDelegaParaServico() {
        when(relatorioIAService.buscar(1L)).thenReturn(rel(1L));

        assertThat(controller.buscar(1L)).isNotNull();
    }

    @Test
    void listarDelegaParaServico() {
        when(relatorioIAService.listar(10L)).thenReturn(List.of(rel(1L), rel(2L)));

        assertThat(controller.listar(10L)).hasSize(2);
    }

    @Test
    void atualizarDelegaParaServico() {
        var auth = auth(user(10L, ALUNO));
        var req = new RelatorioIAUpdateRequest("Conteúdo atualizado");
        var resp = new RelatorioIAResponse(1L, RelatorioStatus.CONCLUIDO, "Conteúdo atualizado", null);
        when(relatorioIAService.atualizar(1L, "Conteúdo atualizado", auth)).thenReturn(resp);

        assertThat(controller.atualizar(1L, req, auth).getStatus()).isEqualTo(RelatorioStatus.CONCLUIDO);
    }

    @Test
    void excluirDelegaParaServico() {
        controller.excluir(1L);
        verify(relatorioIAService).excluir(1L);
    }
}
