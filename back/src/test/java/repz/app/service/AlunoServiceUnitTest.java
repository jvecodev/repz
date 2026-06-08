package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.AlunoCreateRequest;
import repz.app.dto.request.AlunoMeUpdateRequest;
import repz.app.dto.request.AlunoUpdateRequest;
import repz.app.dto.response.AlunoDetalheResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.*;
import repz.app.persistence.repository.*;
import repz.app.service.academia.AcademiaContextService;
import repz.app.service.aluno.AlunoServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.personal;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class AlunoServiceUnitTest {

    @Mock private AlunoRepository alunoRepository;
    @Mock private UserRepository userRepository;
    @Mock private AcademiaRepository academiaRepository;
    @Mock private PersonalRepository personalRepository;
    @Mock private PlanoRepository planoRepository;
    @Mock private AcademiaContextService academiaContextService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Mensagens mensagens;

    @InjectMocks
    private AlunoServiceImpl service;

    // ─── matricular ───────────────────────────────────────────────────────────

    @Test
    void matricularAlunoComPlanoCorreto() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User alunoUser = user(20L, UserRole.ALUNO);
        Plano plano = criarPlano(5, academia);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(userRepository.findByIdAndDeletedAtIsNull(alunoUser.getId())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.existsByUsuarioIdAndAcademiaId(alunoUser.getId(), academia.getId())).thenReturn(false);
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(planoRepository.findById(plano.getId())).thenReturn(Optional.of(plano));
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(inv -> {
            Aluno a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        var req = new AlunoCreateRequest(alunoUser.getId(), plano.getId(), null, "Hipertrofia");
        AlunoDetalheResponse resp = service.matricular(req, null, auth(acadUser.getEmail()));

        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getNome()).isEqualTo(alunoUser.getName());
        assertThat(resp.getObjetivo()).isEqualTo("Hipertrofia");

        ArgumentCaptor<Aluno> captor = ArgumentCaptor.forClass(Aluno.class);
        verify(alunoRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    @Test
    void matricularAlunoComPersonalVinculado() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User alunoUser = user(20L, UserRole.ALUNO);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);
        Plano plano = criarPlano(5, academia);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(userRepository.findByIdAndDeletedAtIsNull(alunoUser.getId())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.existsByUsuarioIdAndAcademiaId(alunoUser.getId(), academia.getId())).thenReturn(false);
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(planoRepository.findById(plano.getId())).thenReturn(Optional.of(plano));
        when(personalRepository.findById(personal.getId())).thenReturn(Optional.of(personal));
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(inv -> {
            Aluno a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        var req = new AlunoCreateRequest(alunoUser.getId(), plano.getId(), personal.getId(), null);
        AlunoDetalheResponse resp = service.matricular(req, null, auth(acadUser.getEmail()));

        assertThat(resp.getPersonalId()).isEqualTo(personal.getId());
        assertThat(resp.getPersonalNome()).isEqualTo(personalUser.getName());
    }

    @Test
    void matricularRejeitaUserComRoleErrada() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(userRepository.findByIdAndDeletedAtIsNull(personalUser.getId())).thenReturn(Optional.of(personalUser));
        when(mensagens.get(any())).thenReturn("role invalida");

        var req = new AlunoCreateRequest(personalUser.getId(), 5, null, null);
        assertThrows(AccessDeniedException.class, () -> service.matricular(req, null, auth(acadUser.getEmail())));
        verify(alunoRepository, never()).save(any());
    }

    @Test
    void matricularRejeitaDuplicata() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User alunoUser = user(20L, UserRole.ALUNO);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(userRepository.findByIdAndDeletedAtIsNull(alunoUser.getId())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.existsByUsuarioIdAndAcademiaId(alunoUser.getId(), academia.getId())).thenReturn(true);
        when(mensagens.get(any())).thenReturn("ja matriculado");

        var req = new AlunoCreateRequest(alunoUser.getId(), 5, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.matricular(req, null, auth(acadUser.getEmail())));
        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void matricularRejeitaPlanoDeOutraAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Academia outraAcademia = academia(11L, user(9L, UserRole.GERENTE));
        User alunoUser = user(20L, UserRole.ALUNO);
        Plano planoOutra = criarPlano(5, outraAcademia);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(userRepository.findByIdAndDeletedAtIsNull(alunoUser.getId())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.existsByUsuarioIdAndAcademiaId(alunoUser.getId(), academia.getId())).thenReturn(false);
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(planoRepository.findById(planoOutra.getId())).thenReturn(Optional.of(planoOutra));
        when(mensagens.get(any())).thenReturn("plano nao pertence");

        var req = new AlunoCreateRequest(alunoUser.getId(), planoOutra.getId(), null, null);
        assertThrows(AccessDeniedException.class, () -> service.matricular(req, null, auth(acadUser.getEmail())));
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Test
    void adminListaTodosAlunos() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveOptional(auth(admin.getEmail()), null)).thenReturn(null);
        when(alunoRepository.findAll()).thenReturn(List.of(aluno));

        List<AlunoDetalheResponse> result = service.findAll(null, auth(admin.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void adminFiltraPorAcademia() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveOptional(auth(admin.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(alunoRepository.findByAcademiaId(academia.getId())).thenReturn(List.of(aluno));

        List<AlunoDetalheResponse> result = service.findAll(academia.getId(), auth(admin.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void gerenteVeSomenteAlunosDaPropriaAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);

        when(userRepository.findByEmail(acadUser.getEmail())).thenReturn(Optional.of(acadUser));
        when(academiaContextService.resolveOptional(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(alunoRepository.findByAcademiaId(academia.getId())).thenReturn(List.of(aluno));

        List<AlunoDetalheResponse> result = service.findAll(null, auth(acadUser.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void personalVeSomenteSeusAlunos() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);
        aluno.setPersonal(personal);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(academiaContextService.resolveOptional(auth(personalUser.getEmail()), null)).thenReturn(null);
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(alunoRepository.findByPersonalId(personal.getId())).thenReturn(List.of(aluno));

        List<AlunoDetalheResponse> result = service.findAll(null, auth(personalUser.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void alunoNaoPodeListarAlunos() {
        User alunoUser = user(20L, UserRole.ALUNO);
        when(userRepository.findByEmail(alunoUser.getEmail())).thenReturn(Optional.of(alunoUser));
        when(academiaContextService.resolveOptional(auth(alunoUser.getEmail()), null)).thenReturn(null);
        when(mensagens.get(any())).thenReturn("acesso negado");

        assertThrows(AccessDeniedException.class, () -> service.findAll(null, auth(alunoUser.getEmail())));
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    void adminBuscaAlunoPorId() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));

        AlunoDetalheResponse resp = service.findById(1L, auth(admin.getEmail()));
        assertThat(resp.getId()).isEqualTo(1L);
    }

    @Test
    void personalVeApenasSeusAlunos() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);
        aluno.setPersonal(personal);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));

        AlunoDetalheResponse resp = service.findById(1L, auth(personalUser.getEmail()));
        assertThat(resp.getId()).isEqualTo(1L);
    }

    @Test
    void personalNaoVeAlunoDeOutroPersonal() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        User outroPersonalUser = user(6L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);
        Personal outroPersonal = personal(31L, outroPersonalUser, academia);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);
        aluno.setPersonal(outroPersonal);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(mensagens.get(any())).thenReturn("acesso negado");

        assertThrows(AccessDeniedException.class, () -> service.findById(1L, auth(personalUser.getEmail())));
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void atualizarMatriculaAlteraPlanoEObjetivo() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);
        Plano novoPlano = criarPlano(7, academia);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(planoRepository.findById(novoPlano.getId())).thenReturn(Optional.of(novoPlano));
        when(alunoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new AlunoUpdateRequest(novoPlano.getId(), null, "Emagrecimento");
        AlunoDetalheResponse resp = service.atualizar(1L, req, null, auth(acadUser.getEmail()));

        assertThat(resp.getObjetivo()).isEqualTo("Emagrecimento");
        assertThat(resp.getPlanoNome()).isEqualTo(novoPlano.getNome());
    }

    @Test
    void atualizarRejeitaAlunoDaOutraAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Academia outraAcademia = academia(11L, user(9L, UserRole.GERENTE));
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), outraAcademia);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(mensagens.get(any())).thenReturn("nao pode editar");

        assertThrows(AccessDeniedException.class,
                () -> service.atualizar(1L, new AlunoUpdateRequest(null, null, "x"), null, auth(acadUser.getEmail())));
    }

    // ─── inativar ─────────────────────────────────────────────────────────────

    @Test
    void inativarAlunoMudaStatusParaFalse() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), academia);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));

        service.inativar(1L, null, auth(acadUser.getEmail()));

        assertThat(aluno.getAtivo()).isFalse();
        verify(alunoRepository).save(aluno);
    }

    @Test
    void inativarRejeitaAlunoDaOutraAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Academia outraAcademia = academia(11L, user(9L, UserRole.GERENTE));
        Aluno aluno = criarAluno(1L, user(20L, UserRole.ALUNO), outraAcademia);

        when(academiaContextService.resolveRequired(auth(acadUser.getEmail()), null)).thenReturn(academia.getId());
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(mensagens.get(any())).thenReturn("nao pode");

        assertThrows(AccessDeniedException.class, () -> service.inativar(1L, null, auth(acadUser.getEmail())));
        verify(alunoRepository, never()).save(any());
    }

    // ─── obterMeuPerfil ───────────────────────────────────────────────────────

    @Test
    void alunoObtemProprioPerfilPorAutenticacao() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User alunoUser = user(20L, UserRole.ALUNO);
        Aluno aluno = criarAluno(1L, alunoUser, academia);

        when(userRepository.findByEmail(alunoUser.getEmail())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.findByUsuarioId(alunoUser.getId())).thenReturn(Optional.of(aluno));

        AlunoDetalheResponse resp = service.obterMeuPerfil(auth(alunoUser.getEmail()));
        assertThat(resp.getUserId()).isEqualTo(alunoUser.getId());
    }

    @Test
    void obterMeuPerfilLancaExcecaoQuandoNaoMatriculado() {
        User alunoUser = user(20L, UserRole.ALUNO);
        when(userRepository.findByEmail(alunoUser.getEmail())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.findByUsuarioId(alunoUser.getId())).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("nao encontrado");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obterMeuPerfil(auth(alunoUser.getEmail())));
        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // ─── atualizarMeuPerfil ───────────────────────────────────────────────────

    @Test
    void alunoAtualizaProprioNomeETelefone() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User alunoUser = user(20L, UserRole.ALUNO);
        Aluno aluno = criarAluno(1L, alunoUser, academia);

        when(userRepository.findByEmail(alunoUser.getEmail())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.findByUsuarioId(alunoUser.getId())).thenReturn(Optional.of(aluno));
        when(alunoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenReturn(alunoUser);

        var req = new AlunoMeUpdateRequest("Novo Nome", "11999999999", null, null);
        AlunoDetalheResponse resp = service.atualizarMeuPerfil(req, auth(alunoUser.getEmail()));

        assertThat(resp.getNome()).isEqualTo("Novo Nome");
        assertThat(aluno.getTelefone()).isEqualTo("11999999999");
    }

    @Test
    void alunoAtualizaSenha() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User alunoUser = user(20L, UserRole.ALUNO);
        Aluno aluno = criarAluno(1L, alunoUser, academia);

        when(userRepository.findByEmail(alunoUser.getEmail())).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.findByUsuarioId(alunoUser.getId())).thenReturn(Optional.of(aluno));
        when(passwordEncoder.encode("novaSenha")).thenReturn("encodedSenha");
        when(alunoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenReturn(alunoUser);

        service.atualizarMeuPerfil(new AlunoMeUpdateRequest(null, null, null, "novaSenha"), auth(alunoUser.getEmail()));

        assertThat(alunoUser.getPassword()).isEqualTo("encodedSenha");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Plano criarPlano(Integer id, Academia academia) {
        return Plano.builder()
                .id(id).nome("Plano " + id).duracaoDias(30)
                .valor(BigDecimal.valueOf(99.90)).ativo(true).academia(academia)
                .build();
    }

    private Aluno criarAluno(Long id, User usuario, Academia academia) {
        Aluno a = new Aluno();
        a.setId(id); a.setUsuario(usuario); a.setAcademia(academia); a.setAtivo(true);
        return a;
    }
}
