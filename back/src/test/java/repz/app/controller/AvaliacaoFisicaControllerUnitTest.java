package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.controller.impl.AvaliacaoFisicaControllerImpl;
import repz.app.dto.request.AvaliacaoFisicaCreateRequest;
import repz.app.dto.response.AvaliacaoFisicaGraficoResponse;
import repz.app.dto.response.AvaliacaoFisicaResponse;
import repz.app.dto.response.AvaliacaoFisicaUnidadeResponse;
import repz.app.service.avaliacaoFisica.AvaliacaoFisicaService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.persistence.entity.UserRole.ALUNO;
import static repz.app.persistence.entity.UserRole.PERSONAL;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class AvaliacaoFisicaControllerUnitTest {

    @Mock private AvaliacaoFisicaService avaliacaoFisicaService;
    @InjectMocks private AvaliacaoFisicaControllerImpl controller;

    @Test
    void criarDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var req = new AvaliacaoFisicaCreateRequest();
        var resp = new AvaliacaoFisicaResponse();
        when(avaliacaoFisicaService.criar(req, auth)).thenReturn(resp);

        assertThat(controller.criar(req, auth)).isSameAs(resp);
    }

    @Test
    void findAllDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        when(avaliacaoFisicaService.findAll(2L, auth)).thenReturn(List.of(new AvaliacaoFisicaResponse()));

        assertThat(controller.findAll(2L, auth)).hasSize(1);
    }

    @Test
    void findByIdDelegaParaServico() {
        var resp = new AvaliacaoFisicaResponse();
        when(avaliacaoFisicaService.findById(1L)).thenReturn(resp);

        assertThat(controller.findById(1L)).isSameAs(resp);
    }

    @Test
    void obterGraficoDelegaParaServico() {
        var auth = auth(user(2L, ALUNO));
        var resp = org.mockito.Mockito.mock(AvaliacaoFisicaGraficoResponse.class);
        when(avaliacaoFisicaService.obterGrafico(2L, auth)).thenReturn(resp);

        assertThat(controller.obterGrafico(2L, auth)).isSameAs(resp);
    }

    @Test
    void obterDaUnidadeDelegaParaServico() {
        var auth = auth(user(1L, repz.app.persistence.entity.UserRole.GERENTE));
        when(avaliacaoFisicaService.obterDaUnidade(10L, auth)).thenReturn(List.of(new AvaliacaoFisicaUnidadeResponse()));

        assertThat(controller.obterDaUnidade(10L, auth)).hasSize(1);
    }

    @Test
    void ativarDelegaParaServico() {
        controller.ativar(1L);
        verify(avaliacaoFisicaService).ativar(1L);
    }

    @Test
    void desativarDelegaParaServico() {
        controller.desativar(1L);
        verify(avaliacaoFisicaService).desativar(1L);
    }
}
