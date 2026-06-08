package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import repz.app.controller.impl.PlanoControllerImpl;
import repz.app.dto.request.PlanoPostRequest;
import repz.app.dto.request.PlanoPutRequest;
import repz.app.dto.response.PlanoResponse;
import repz.app.service.plano.PlanoService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.persistence.entity.UserRole.GERENTE;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class PlanoControllerUnitTest {

    @Mock private PlanoService planoService;
    @InjectMocks private PlanoControllerImpl controller;

    private PlanoResponse plano(int id) {
        return new PlanoResponse(id, "Mensal", 30, BigDecimal.valueOf(99.90), true);
    }

    @Test
    void criarRetorna201() {
        var auth = auth(user(1L, GERENTE));
        var req = new PlanoPostRequest("Mensal", 30, BigDecimal.valueOf(99.90));

        var resp = controller.criar(req, null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        verify(planoService).criar(req, null, auth);
    }

    @Test
    void findAllRetornaLista() {
        var auth = auth(user(1L, GERENTE));
        when(planoService.findAll(null, auth)).thenReturn(List.of(plano(1), plano(2)));

        var resp = controller.findAll(null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void findByIdRetornaPlano() {
        var auth = auth(user(1L, GERENTE));
        when(planoService.findById(1, null, auth)).thenReturn(plano(1));

        var resp = controller.findById(1, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().nome()).isEqualTo("Mensal");
    }

    @Test
    void atualizarRetorna200() {
        var auth = auth(user(1L, GERENTE));
        var req = new PlanoPutRequest("Trimestral", 90, BigDecimal.valueOf(249.90));

        var resp = controller.atualizar(1, req, null, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(planoService).atualizar(1, req, null, auth);
    }

    @Test
    void ativarRetorna200() {
        var auth = auth(user(1L, GERENTE));
        assertThat(controller.ativar(1, null, auth).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(planoService).ativar(1, null, auth);
    }

    @Test
    void desativarRetorna200() {
        var auth = auth(user(1L, GERENTE));
        assertThat(controller.desativar(1, null, auth).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(planoService).desativar(1, null, auth);
    }
}
