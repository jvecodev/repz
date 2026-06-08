package repz.app.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import repz.app.controller.impl.UserControllerImpl;
import repz.app.dto.request.AdminCreateRequest;
import repz.app.dto.request.UserCreateRequest;
import repz.app.dto.request.UserPutRequest;
import repz.app.dto.request.UserSelfUpdateRequest;
import repz.app.dto.response.UserGetResponse;
import repz.app.persistence.entity.UserRole;
import repz.app.service.user.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    @Mock private UserService userService;
    @InjectMocks private UserControllerImpl controller;

    private UserGetResponse resp(Long id) {
        return new UserGetResponse(id, "User " + id, "u" + id + "@r.com", null, UserRole.ALUNO, true, null);
    }

    @Test
    void findAllRetornaLista() {
        when(userService.findAll()).thenReturn(List.of(resp(1L), resp(2L)));
        var result = controller.findAll();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
    }

    @Test
    void findByIdRetornaUsuario() {
        when(userService.findById(1L)).thenReturn(resp(1L));
        var result = controller.findById(1L);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().id()).isEqualTo(1L);
    }

    @Test
    void criarRetorna201() {
        var auth = auth(user(1L, UserRole.ADMIN));
        var req = new UserCreateRequest("U", "u@r.com", "12345", UserRole.ALUNO, 10L, 1);
        var result = controller.criar(req, auth);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userService).criarUsuario(req, auth);
    }

    @Test
    void criarAdminRetorna201() {
        var req = new AdminCreateRequest("Admin", "a@r.com", "12345");
        var result = controller.criarAdmin(req);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userService).criarAdmin(req);
    }

    @Test
    void obterMeuPerfilRetornaUsuario() {
        var auth = auth(user(1L, UserRole.ALUNO));
        when(userService.obterMeuPerfil(auth)).thenReturn(resp(1L));
        var result = controller.obterMeuPerfil(auth);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void atualizarMeuPerfilRetorna200() {
        var auth = auth(user(1L, UserRole.ALUNO));
        var req = new UserSelfUpdateRequest("Novo", "n@r.com", null);
        var result = controller.atualizarMeuPerfil(req, auth);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).atualizarMeuPerfil(req, auth);
    }

    @Test
    void atualizarRetorna200() {
        var req = new UserPutRequest("Novo", "n@r.com", UserRole.ALUNO, true);
        var result = controller.atualizar(1L, req);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).atualizar(1L, req);
    }

    @Test
    void ativarRetorna200() {
        assertThat(controller.ativar(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).ativar(1L);
    }

    @Test
    void desativarRetorna200() {
        assertThat(controller.desativar(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).desativar(1L);
    }
}
