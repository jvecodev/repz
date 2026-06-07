package repz.app.service;

import io.minio.ObjectWriteResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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

import java.io.InputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        userService.atualizar(user.getId(), new UserPutRequest(
                "Nome Atualizado",
                "nao-usado@repz.com",
                UserRole.ADMIN,
                false));
        userService.desativar(user.getId());

        var desativado = userRepository.findById(user.getId()).orElseThrow();
        assertThat(desativado.getName()).isEqualTo("Nome Atualizado");
        assertThat(desativado.getActive()).isFalse();
        assertThat(desativado.getDeletedAt()).isNotNull();

        userService.ativar(user.getId());

        var reativado = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reativado.getActive()).isTrue();
        assertThat(reativado.getDeletedAt()).isNull();
    }

    @Test
    void findByIdIgnoraUsuarioDeletado() {
        var user = criarUsuario(UserRole.ALUNO, "usuario-deletado");
        userService.desativar(user.getId());

        assertThatThrownBy(() -> userService.findById(user.getId()))
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

    // ── atualizarFotoPerfil ──────────────────────────────────────────────────

    @Test
    void atualizarFotoPerfilSalvaUrlNoUser() throws Exception {
        var user = criarUsuario(UserRole.ADMIN, "foto-admin");
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        MultipartFile foto = mock(MultipartFile.class);
        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(1024L);
        when(foto.getContentType()).thenReturn("image/jpeg");
        when(foto.getOriginalFilename()).thenReturn("avatar.jpg");
        when(foto.getInputStream()).thenReturn(InputStream.nullInputStream());

        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));
        when(minioClient.getPresignedObjectUrl(any()))
                .thenReturn("http://minio:9000/repz/users/" + user.getId() + "/avatar.jpg?token=abc");

        var response = userService.atualizarFotoPerfil(foto, auth);

        assertThat(response.fotoUrl()).startsWith("http://localhost:9000/");
        var saved = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(saved.getFotoUrl()).startsWith("http://localhost:9000/");
    }

    @Test
    void atualizarFotoPerfilAtualizaAlunoTambem() throws Exception {
        var gerenteUser = criarUsuario(UserRole.GERENTE, "gerente-foto");
        var academia = criarAcademia(gerenteUser, "academia-foto");
        var plano = criarPlano(academia, "Plano-foto");
        var alunoUser = criarUsuario(UserRole.ALUNO, "aluno-foto");

        repz.app.persistence.entity.Aluno aluno = new repz.app.persistence.entity.Aluno();
        aluno.setUsuario(alunoUser);
        aluno.setAcademia(academia);
        aluno.setPlano(plano);
        aluno.setDataInicio(java.time.LocalDate.now());
        aluno.setAtivo(true);
        alunoRepository.saveAndFlush(aluno);

        var auth = new UsernamePasswordAuthenticationToken(alunoUser, null, alunoUser.getAuthorities());

        MultipartFile foto = mock(MultipartFile.class);
        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(2048L);
        when(foto.getContentType()).thenReturn("image/png");
        when(foto.getOriginalFilename()).thenReturn("avatar.png");
        when(foto.getInputStream()).thenReturn(InputStream.nullInputStream());

        when(minioClient.putObject(any())).thenReturn(mock(ObjectWriteResponse.class));
        when(minioClient.getPresignedObjectUrl(any()))
                .thenReturn("http://minio:9000/repz/users/" + alunoUser.getId() + "/avatar.png?token=xyz");

        userService.atualizarFotoPerfil(foto, auth);

        var savedAluno = alunoRepository.findByUsuarioId(alunoUser.getId()).orElseThrow();
        assertThat(savedAluno.getFotoUrl()).startsWith("http://localhost:9000/");
    }

    @Test
    void atualizarFotoPerfilRejeitaArquivoVazio() {
        var user = criarUsuario(UserRole.GERENTE, "foto-vazio");
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        MultipartFile foto = mock(MultipartFile.class);
        when(foto.isEmpty()).thenReturn(true);

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userService.atualizarFotoPerfil(foto, auth));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void atualizarFotoPerfilRejeitaFormatoInvalido() {
        var user = criarUsuario(UserRole.PERSONAL, "foto-formato");
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        MultipartFile foto = mock(MultipartFile.class);
        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(1024L);
        when(foto.getContentType()).thenReturn("image/gif");

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userService.atualizarFotoPerfil(foto, auth));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void atualizarFotoPerfilRejeitaTamanhoExcedido() {
        var user = criarUsuario(UserRole.ALUNO, "foto-tamanho");
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        MultipartFile foto = mock(MultipartFile.class);
        when(foto.isEmpty()).thenReturn(false);
        when(foto.getSize()).thenReturn(6L * 1024 * 1024); // 6 MB > 5 MB limit

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userService.atualizarFotoPerfil(foto, auth));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void atualizarFotoPerfilRejeitaAutenticacaoNula() {
        MultipartFile foto = mock(MultipartFile.class);

        org.junit.jupiter.api.Assertions.assertThrows(
                AccessDeniedException.class,
                () -> userService.atualizarFotoPerfil(foto, null));
    }
}
