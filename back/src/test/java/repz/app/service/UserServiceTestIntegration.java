package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.AdminCreateRequest;
import repz.app.dto.request.UserCreateRequest;
import repz.app.dto.request.UserPutRequest;
import repz.app.persistence.entity.Plano;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AlunoRepository;
import repz.app.persistence.repository.PlanoRepository;
import repz.app.service.user.UserDetailsServiceImpl;
import repz.app.service.user.UserService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private PlanoRepository planoRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    @Test
    void criarUsuarioAluno() {
        var gerenteUser = criarUsuario(UserRole.GERENTE, "gerente-aluno");
        var academia = criarAcademia(gerenteUser, "academia-aluno");
        var plano = criarPlano(academia, "Mensal");

        userService.criarUsuario(new UserCreateRequest(
                "Aluno Teste", "aluno-novo@repz.com", "123456",
                UserRole.ALUNO, academia.getId(), plano.getId()), null);

        var saved = userRepository.findByEmail("aluno-novo@repz.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.ALUNO);
        assertThat(saved.getActive()).isTrue();
        assertThat(alunoRepository.existsByUsuarioIdAndAcademiaId(saved.getId(), academia.getId())).isTrue();
    }

    @Test
    void criarUsuarioPersonal() {
        var gerenteUser = criarUsuario(UserRole.GERENTE, "gerente-personal-criar");
        var academia = criarAcademia(gerenteUser, "academia-personal-criar");

        userService.criarUsuario(new UserCreateRequest(
                "Personal Novo", "personal-novo@repz.com", "123456",
                UserRole.PERSONAL, academia.getId(), null), null);

        assertThat(userRepository.findByEmail("personal-novo@repz.com").orElseThrow().getRole())
                .isEqualTo(UserRole.PERSONAL);
    }

    @Test
    void criarUsuarioRejeitaRoleAdmin() {
        var gerenteUser = criarUsuario(UserRole.GERENTE, "gerente-admin-rejeita");
        var academia = criarAcademia(gerenteUser, "academia-admin-rejeita");

        assertThatThrownBy(() -> userService.criarUsuario(new UserCreateRequest(
                "Admin Tentativa", "admin-rejeita@repz.com", "123456",
                UserRole.ADMIN, academia.getId(), null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void criarAdmin() {
        userService.criarAdmin(new AdminCreateRequest("Admin Novo", "admin-novo@repz.com", "123456"));

        var saved = userRepository.findByEmail("admin-novo@repz.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    void atualizarDesativarEAtivarUsuario() {
        var user = criarUsuario(UserRole.ALUNO, "usuario-status");

        userService.atualizar(Math.toIntExact(user.getId()), new UserPutRequest(
                "Nome Atualizado",
                "nao-usado@repz.com",
                UserRole.ADMIN,
                false));
        userService.desativar(Math.toIntExact(user.getId()));

        var desativado = userRepository.findById(Math.toIntExact(user.getId())).orElseThrow();
        assertThat(desativado.getName()).isEqualTo("Nome Atualizado");
        assertThat(desativado.getActive()).isFalse();
        assertThat(desativado.getDeletedAt()).isNotNull();

        userService.ativar(Math.toIntExact(user.getId()));

        var reativado = userRepository.findById(Math.toIntExact(user.getId())).orElseThrow();
        assertThat(reativado.getActive()).isTrue();
        assertThat(reativado.getDeletedAt()).isNull();
    }

    @Test
    void findByIdIgnoraUsuarioDeletado() {
        var user = criarUsuario(UserRole.ALUNO, "usuario-deletado");
        userService.desativar(Math.toIntExact(user.getId()));

        assertThatThrownBy(() -> userService.findById(Math.toIntExact(user.getId())))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void userDetailsCarregaPorEmailERejeitaInexistente() {
        var user = criarUsuario(UserRole.ALUNO, "userdetails");

        assertThat(userDetailsService.loadUserByUsername(user.getEmail()).getUsername())
                .isEqualTo(user.getEmail());
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ausente@repz.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    private Plano criarPlano(repz.app.persistence.entity.Academia academia, String nome) {
        Plano plano = Plano.builder()
                .nome(nome)
                .duracaoDias(30)
                .valor(BigDecimal.valueOf(99.90))
                .ativo(true)
                .academia(academia)
                .build();
        return planoRepository.saveAndFlush(plano);
    }
}
