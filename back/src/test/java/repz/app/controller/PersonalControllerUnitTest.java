package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.controller.impl.PersonalControllerImpl;
import repz.app.dto.request.PersonalUpdateRequest;
import repz.app.dto.response.PersonalAlunosResponse;
import repz.app.dto.response.PersonalResponse;
import repz.app.service.personal.PersonalService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.persistence.entity.UserRole.PERSONAL;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class PersonalControllerUnitTest {

    @Mock private PersonalService personalService;
    @InjectMocks private PersonalControllerImpl controller;

    @Test
    void findAllDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var resp = new PersonalResponse(1L, 1L, "P", "p@r.com", 10L, "A", "Funcional", true, null);
        when(personalService.findAll(null, auth)).thenReturn(List.of(resp));

        assertThat(controller.findAll(null, auth)).hasSize(1);
        verify(personalService).findAll(null, auth);
    }

    @Test
    void findByIdDelegaParaServico() {
        var resp = new PersonalResponse(1L, 1L, "P", "p@r.com", 10L, "A", "Funcional", true, null);
        when(personalService.findById(1L)).thenReturn(resp);

        assertThat(controller.findById(1L)).isSameAs(resp);
    }

    @Test
    void atualizarDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var req = new PersonalUpdateRequest("CrossFit", true);
        var resp = new PersonalResponse(1L, 1L, "P", "p@r.com", 10L, "A", "CrossFit", true, null);
        when(personalService.atualizar(1L, req, null, auth)).thenReturn(resp);

        assertThat(controller.atualizar(1L, req, null, auth)).isSameAs(resp);
    }

    @Test
    void ativarDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var resp = new PersonalResponse(1L, 1L, "P", "p@r.com", 10L, "A", "F", true, null);
        when(personalService.ativar(1L, null, auth)).thenReturn(resp);

        assertThat(controller.ativar(1L, null, auth)).isSameAs(resp);
    }

    @Test
    void desativarDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var resp = new PersonalResponse(1L, 1L, "P", "p@r.com", 10L, "A", "F", false, null);
        when(personalService.desativar(1L, null, auth)).thenReturn(resp);

        assertThat(controller.desativar(1L, null, auth)).isSameAs(resp);
    }

    @Test
    void obterMeuPerfilDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var resp = new PersonalResponse(1L, 1L, "P", "p@r.com", 10L, "A", "F", true, null);
        when(personalService.obterMeuPerfil(auth)).thenReturn(resp);

        assertThat(controller.obterMeuPerfil(auth)).isSameAs(resp);
    }

    @Test
    void atualizarMeuPerfilDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var req = new PersonalUpdateRequest("Yoga", null);
        var resp = new PersonalResponse(1L, 1L, "P", "p@r.com", 10L, "A", "Yoga", true, null);
        when(personalService.atualizarMeuPerfil(req, auth)).thenReturn(resp);

        assertThat(controller.atualizarMeuPerfil(req, auth)).isSameAs(resp);
    }

    @Test
    void obterMeusAlunosDelegaParaServico() {
        var auth = auth(user(1L, PERSONAL));
        var resp = new PersonalAlunosResponse(1L, "P", "F", 10L, "A", List.of());
        when(personalService.obterMeusAlunos(auth)).thenReturn(resp);

        assertThat(controller.obterMeusAlunos(auth)).isSameAs(resp);
    }
}
