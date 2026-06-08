package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.controller.impl.FrequenciaControllerImpl;
import repz.app.dto.request.FrequenciaCreateRequest;
import repz.app.dto.response.AlunoInativoResponse;
import repz.app.dto.response.FrequenciaRelatorioResponse;
import repz.app.dto.response.FrequenciaResponse;
import repz.app.message.Mensagens;
import repz.app.service.frequencia.FrequenciaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.persistence.entity.UserRole.ALUNO;
import static repz.app.persistence.entity.UserRole.GERENTE;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class FrequenciaControllerUnitTest {

    @Mock private FrequenciaService frequenciaService;
    @Mock private Mensagens mensagens;
    @InjectMocks private FrequenciaControllerImpl controller;

    @Test
    void criarDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        var req = new FrequenciaCreateRequest(1L, 10L, null, null);
        var resp = org.mockito.Mockito.mock(FrequenciaResponse.class);
        when(frequenciaService.criar(req, null, auth)).thenReturn(resp);

        assertThat(controller.criar(req, null, auth)).isSameAs(resp);
    }

    @Test
    void findAllComAlunoFiltrarPorPeriodo() {
        var auth = auth(user(1L, GERENTE));
        var inicio = LocalDateTime.now().minusDays(7);
        var fim = LocalDateTime.now();
        when(frequenciaService.filtrarPorPeriodo(1L, 10L, inicio, fim, auth)).thenReturn(List.of());

        var result = controller.findAll(1L, 10L, inicio, fim, null, auth);
        assertThat(result).isEmpty();
        verify(frequenciaService).filtrarPorPeriodo(1L, 10L, inicio, fim, auth);
    }

    @Test
    void findAllComAcademiaHeaderPrecedeQuery() {
        var auth = auth(user(1L, GERENTE));
        var inicio = LocalDateTime.now().minusDays(7);
        var fim = LocalDateTime.now();
        // headerAcademiaId=20L deve prevalecer sobre academiaId=10L
        when(frequenciaService.filtrarPorAcademiaEPeriodo(20L, inicio, fim, auth)).thenReturn(List.of());

        controller.findAll(null, 10L, inicio, fim, 20L, auth);
        verify(frequenciaService).filtrarPorAcademiaEPeriodo(20L, inicio, fim, auth);
    }

    @Test
    void findAllSemFiltroDelegaParaServico() {
        var auth = auth(user(1L, GERENTE));
        var inicio = LocalDateTime.now().minusDays(7);
        var fim = LocalDateTime.now();
        when(frequenciaService.filtrarPorAcademiaEPeriodo(10L, inicio, fim, auth)).thenReturn(List.of());

        controller.findAll(null, 10L, inicio, fim, null, auth);
        verify(frequenciaService).filtrarPorAcademiaEPeriodo(10L, inicio, fim, auth);
    }

    @Test
    void findAllSemFiltroLancaExcecao() {
        var auth = auth(user(1L, GERENTE));
        when(mensagens.get(any())).thenReturn("filtro obrigatorio");

        assertThrows(RuntimeException.class,
                () -> controller.findAll(null, null, null, null, null, auth));
    }

    @Test
    void findByIdDelegaParaServico() {
        var resp = org.mockito.Mockito.mock(FrequenciaResponse.class);
        when(frequenciaService.findById(1L)).thenReturn(resp);

        assertThat(controller.findById(1L)).isSameAs(resp);
    }

    @Test
    void meuHistoricoDelegaParaServico() {
        var auth = auth(user(1L, ALUNO));
        when(frequenciaService.meuHistorico(auth)).thenReturn(List.of());

        assertThat(controller.meuHistorico(auth)).isEmpty();
    }

    @Test
    void alunosInativosComHeaderPrecedeQuery() {
        var auth = auth(user(1L, GERENTE));
        when(frequenciaService.obterAlunosInativos(20L, auth)).thenReturn(List.of());

        controller.alunosInativos(10L, 20L, auth);
        verify(frequenciaService).obterAlunosInativos(20L, auth);
    }

    @Test
    void obterRelatorioComHeaderPrecedeQuery() {
        var auth = auth(user(1L, GERENTE));
        var inicio = LocalDateTime.now().minusDays(7);
        var fim = LocalDateTime.now();
        var resp = org.mockito.Mockito.mock(FrequenciaRelatorioResponse.class);
        when(frequenciaService.obterRelatorio(20L, inicio, fim, auth)).thenReturn(resp);

        assertThat(controller.obterRelatorio(10L, inicio, fim, 20L, auth)).isSameAs(resp);
    }

    @Test
    void ativarDelegaParaServico() {
        controller.ativar(1L);
        verify(frequenciaService).ativar(1L);
    }

    @Test
    void desativarDelegaParaServico() {
        controller.desativar(1L);
        verify(frequenciaService).desativar(1L);
    }
}
