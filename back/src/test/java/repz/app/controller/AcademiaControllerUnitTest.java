package repz.app.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import repz.app.controller.impl.AcademiaControllerImpl;
import repz.app.dto.request.AcademiaCreateRequest;
import repz.app.dto.request.AcademiaUpdateRequest;
import repz.app.dto.response.AcademiaDashboardResponse;
import repz.app.dto.response.AcademiaResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.service.academia.AcademiaService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class AcademiaControllerUnitTest {

    @Mock private AcademiaService academiaService;
    @Mock private Mensagens mensagens;
    @InjectMocks private AcademiaControllerImpl controller;

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginAs(UserRole role) {
        User u = user(1L, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    private AcademiaResponse resp() {
        return new AcademiaResponse(1L, "12345678000100", "G", "R 1", "R", null, null, true, 0, 0, null, null);
    }

    @Test
    void adminPodeCriar() {
        loginAs(UserRole.ADMIN);
        when(academiaService.criar(any())).thenReturn(resp());
        var result = controller.criar(new AcademiaCreateRequest());
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void naoAdminNaoPodeCriar() {
        loginAs(UserRole.GERENTE);
        when(mensagens.get(any())).thenReturn("acesso negado");
        assertThrows(AccessDeniedException.class, () -> controller.criar(new AcademiaCreateRequest()));
    }

    @Test
    void adminPodeListar() {
        loginAs(UserRole.ADMIN);
        when(academiaService.findAll()).thenReturn(java.util.List.of(resp()));
        var result = controller.findAll();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void adminPodeBuscarPorId() {
        loginAs(UserRole.ADMIN);
        when(academiaService.findById(1L)).thenReturn(resp());
        assertThat(controller.findById(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminPodeAtualizar() {
        loginAs(UserRole.ADMIN);
        when(academiaService.atualizar(any(), any())).thenReturn(resp());
        assertThat(controller.atualizar(1L, new AcademiaUpdateRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminPodeAtivar() {
        loginAs(UserRole.ADMIN);
        when(academiaService.ativar(1L)).thenReturn(resp());
        assertThat(controller.ativar(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminPodeDesativar() {
        loginAs(UserRole.ADMIN);
        when(academiaService.desativar(1L)).thenReturn(resp());
        assertThat(controller.desativar(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void gerentePodeObterMinha() {
        loginAs(UserRole.GERENTE);
        User gerente = user(1L, UserRole.GERENTE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(gerente, null, gerente.getAuthorities()));
        when(academiaService.obterMinha(gerente)).thenReturn(resp());
        assertThat(controller.obterMinha().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminNaoPodeObterMinha() {
        loginAs(UserRole.ADMIN);
        when(mensagens.get(any())).thenReturn("acesso negado");
        assertThrows(AccessDeniedException.class, () -> controller.obterMinha());
    }

    @Test
    void gerentePodeAtualizarMinha() {
        User gerente = user(1L, UserRole.GERENTE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(gerente, null, gerente.getAuthorities()));
        when(academiaService.atualizarMinha(any(), any())).thenReturn(resp());
        assertThat(controller.atualizarMinha(new AcademiaUpdateRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminPodeObterDashboard() {
        loginAs(UserRole.ADMIN);
        when(academiaService.obterDashboard()).thenReturn(new AcademiaDashboardResponse(1L, 1, 0, 5, 2, 2.5));
        assertThat(controller.obterDashboard().getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
