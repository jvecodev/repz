package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.PersonalCreateRequest;
import repz.app.dto.request.PersonalUpdateRequest;
import repz.app.dto.response.PersonalAlunosResponse;
import repz.app.dto.response.PersonalResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Aluno;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.AlunoRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.academia.AcademiaContextService;
import repz.app.service.personal.PersonalServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.personal;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class PersonalServiceUnitTest {

    @Mock private PersonalRepository personalRepository;
    @Mock private UserRepository userRepository;
    @Mock private AcademiaRepository academiaRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private AcademiaContextService academiaContextService;
    @Mock private Mensagens mensagens;

    @InjectMocks
    private PersonalServiceImpl service;

    // ─── criar ───────────────────────────────────────────────────────────────

    @Test
    void adminPodeCriarPersonalParaQualquerAcademia() {
        User admin = user(1L, UserRole.ADMIN);
        User personalUser = user(2L, UserRole.PERSONAL);
        User academiaUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        PersonalCreateRequest request = new PersonalCreateRequest(personalUser.getId(), academia.getId(), "Funcional");

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveRequired(auth(admin.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(userRepository.findById(personalUser.getId())).thenReturn(Optional.of(personalUser));
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.empty());
        when(personalRepository.save(any(Personal.class))).thenAnswer(inv -> {
            Personal p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        PersonalResponse response = service.criar(request, null, auth(admin.getEmail()));

        assertThat(response.getUserId()).isEqualTo(personalUser.getId());
        assertThat(response.getAcademiaId()).isEqualTo(academia.getId());
        assertThat(response.getEspecialidade()).isEqualTo("Funcional");
        verify(personalRepository).save(any(Personal.class));
    }

    @Test
    void gerencePodeCriarPersonalNaPropriaAcademia() {
        User academiaUser = user(3L, UserRole.GERENTE);
        User personalUser = user(2L, UserRole.PERSONAL);
        Academia academia = academia(10L, academiaUser);
        PersonalCreateRequest request = new PersonalCreateRequest(personalUser.getId(), academia.getId(), "Pilates");

        when(userRepository.findByEmail(academiaUser.getEmail())).thenReturn(Optional.of(academiaUser));
        when(academiaContextService.resolveRequired(auth(academiaUser.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(userRepository.findById(personalUser.getId())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.empty());
        when(personalRepository.save(any(Personal.class))).thenAnswer(inv -> {
            Personal p = inv.getArgument(0);
            p.setId(88L);
            return p;
        });

        PersonalResponse response = service.criar(request, null, auth(academiaUser.getEmail()));
        assertThat(response.getAcademiaId()).isEqualTo(academia.getId());
    }

    @Test
    void academiaPodeCriarPersonalApenasNaPropriaAcademia() {
        User academiaUser = user(3L, UserRole.GERENTE);
        User otherAcademiaUser = user(4L, UserRole.GERENTE);
        User personalUser = user(2L, UserRole.PERSONAL);
        Academia outraAcademia = academia(20L, otherAcademiaUser);
        PersonalCreateRequest request = new PersonalCreateRequest(personalUser.getId(), outraAcademia.getId(), "Funcional");

        when(userRepository.findByEmail(academiaUser.getEmail())).thenReturn(Optional.of(academiaUser));
        when(academiaContextService.resolveRequired(auth(academiaUser.getEmail()), outraAcademia.getId())).thenReturn(outraAcademia.getId());
        when(academiaRepository.findById(outraAcademia.getId())).thenReturn(Optional.of(outraAcademia));
        when(mensagens.get(any())).thenReturn("nao pode");

        assertThrows(RuntimeException.class, () -> service.criar(request, null, auth(academiaUser.getEmail())));
    }

    @Test
    void usuarioNaoPodeCriarPersonal() {
        User aluno = user(5L, UserRole.ALUNO);
        PersonalCreateRequest request = new PersonalCreateRequest(2L, 10L, "Funcional");
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(academiaContextService.resolveRequired(auth(aluno.getEmail()), 10L)).thenReturn(10L);
        when(mensagens.get(any())).thenReturn("acesso negado");

        assertThrows(RuntimeException.class, () -> service.criar(request, null, auth(aluno.getEmail())));
    }

    @Test
    void criarRejeitaPersonalJaVinculado() {
        User admin = user(1L, UserRole.ADMIN);
        User personalUser = user(2L, UserRole.PERSONAL);
        User academiaUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        Personal existente = personal(50L, personalUser, academia);
        PersonalCreateRequest request = new PersonalCreateRequest(personalUser.getId(), academia.getId(), "Funcional");

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveRequired(auth(admin.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(userRepository.findById(personalUser.getId())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(existente));
        when(mensagens.get(any())).thenReturn("ja vinculado");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.criar(request, null, auth(admin.getEmail())));
        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Test
    void adminListaTodosPersonais() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal p1 = personal(20L, user(2L, UserRole.PERSONAL), academia);
        Personal p2 = personal(21L, user(6L, UserRole.PERSONAL), academia);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveOptional(auth(admin.getEmail()), null)).thenReturn(null);
        when(personalRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PersonalResponse> result = service.findAll(null, auth(admin.getEmail()));
        assertThat(result).hasSize(2);
    }

    @Test
    void adminFiltraPorAcademiaQuandoInformado() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia1 = academia(10L, acadUser);
        Academia academia2 = academia(11L, acadUser);
        Personal p1 = personal(20L, user(2L, UserRole.PERSONAL), academia1);
        Personal p2 = personal(21L, user(6L, UserRole.PERSONAL), academia2);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveOptional(auth(admin.getEmail()), academia1.getId())).thenReturn(academia1.getId());
        when(personalRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PersonalResponse> result = service.findAll(academia1.getId(), auth(admin.getEmail()));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAcademiaId()).isEqualTo(academia1.getId());
    }

    @Test
    void gerenteVeSomentePersonaisDaPropriaAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal p1 = personal(20L, user(2L, UserRole.PERSONAL), academia);

        when(userRepository.findByEmail(acadUser.getEmail())).thenReturn(Optional.of(acadUser));
        when(academiaContextService.resolveOptional(auth(acadUser.getEmail()), null)).thenReturn(null);
        when(academiaRepository.findByResponsibleUserId(acadUser.getId())).thenReturn(List.of(academia));
        when(personalRepository.findAll()).thenReturn(List.of(p1));

        List<PersonalResponse> result = service.findAll(null, auth(acadUser.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void alunoNaoPodeListarPersonais() {
        User aluno = user(5L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(academiaContextService.resolveOptional(auth(aluno.getEmail()), null)).thenReturn(null);
        when(mensagens.get(any())).thenReturn("acesso negado");

        assertThrows(RuntimeException.class, () -> service.findAll(null, auth(aluno.getEmail())));
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    void findByIdRetornaPersonal() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        when(personalRepository.findById(20L)).thenReturn(Optional.of(personal));

        PersonalResponse response = service.findById(20L);
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getUserId()).isEqualTo(personalUser.getId());
    }

    @Test
    void findByIdLancaExcecaoQuandoNaoExiste() {
        when(personalRepository.findById(999L)).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("not found");

        assertThrows(RuntimeException.class, () -> service.findById(999L));
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void adminAtualizaEspecialidadeDoPersonal() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveOptional(auth(admin.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(personal));
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonalUpdateRequest req = new PersonalUpdateRequest("CrossFit", true);
        PersonalResponse response = service.atualizar(20L, req, null, auth(admin.getEmail()));

        assertThat(response.getEspecialidade()).isEqualTo("CrossFit");
        verify(personalRepository).save(personal);
    }

    @Test
    void atualizarRejeitaQuandoNaoAdminNemGerente() {
        User aluno = user(5L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(academiaContextService.resolveOptional(auth(aluno.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(personal));
        when(mensagens.get(any())).thenReturn("acesso negado");

        PersonalUpdateRequest req = new PersonalUpdateRequest("CrossFit", true);
        assertThrows(RuntimeException.class, () -> service.atualizar(20L, req, null, auth(aluno.getEmail())));
    }

    // ─── ativar / desativar ───────────────────────────────────────────────────

    @Test
    void adminAtivaPersonal() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, user(2L, UserRole.PERSONAL), academia);
        personal.setAtivo(false);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveOptional(auth(admin.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(personal));
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonalResponse response = service.ativar(20L, null, auth(admin.getEmail()));
        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void adminDesativaPersonal() {
        User admin = user(1L, UserRole.ADMIN);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, user(2L, UserRole.PERSONAL), academia);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveOptional(auth(admin.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(personal));
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonalResponse response = service.desativar(20L, null, auth(admin.getEmail()));
        assertThat(response.getAtivo()).isFalse();
    }

    // ─── obterMeuPerfil ───────────────────────────────────────────────────────

    @Test
    void personalObtemProprioPerfilPorEmail() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));

        PersonalResponse response = service.obterMeuPerfil(auth(personalUser.getEmail()));
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getUserId()).isEqualTo(personalUser.getId());
    }

    @Test
    void obterMeuPerfilLancaExcecaoQuandoNaoVinculado() {
        User personalUser = user(2L, UserRole.PERSONAL);
        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of());
        when(mensagens.get(any())).thenReturn("personal nao encontrado");

        assertThrows(RuntimeException.class, () -> service.obterMeuPerfil(auth(personalUser.getEmail())));
    }

    // ─── atualizarMeuPerfil ───────────────────────────────────────────────────

    @Test
    void personalAtualizaPropriaEspecialidade() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonalUpdateRequest req = new PersonalUpdateRequest("Yoga", null);
        PersonalResponse response = service.atualizarMeuPerfil(req, auth(personalUser.getEmail()));
        assertThat(response.getEspecialidade()).isEqualTo("Yoga");
    }

    // ─── gerente ativa/desativa/atualiza ─────────────────────────────────────

    @Test
    void gerenteAtivaPersonalDaPropriaAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal p = personal(20L, user(2L, UserRole.PERSONAL), academia);
        p.setAtivo(false);

        when(userRepository.findByEmail(acadUser.getEmail())).thenReturn(Optional.of(acadUser));
        when(academiaContextService.resolveOptional(auth(acadUser.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(p));
        when(academiaRepository.findByResponsibleUserId(acadUser.getId())).thenReturn(List.of(academia));
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.ativar(20L, null, auth(acadUser.getEmail()));
        assertThat(resp.getAtivo()).isTrue();
    }

    @Test
    void gerenteDesativaPersonalDaPropriaAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal p = personal(20L, user(2L, UserRole.PERSONAL), academia);

        when(userRepository.findByEmail(acadUser.getEmail())).thenReturn(Optional.of(acadUser));
        when(academiaContextService.resolveOptional(auth(acadUser.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(p));
        when(academiaRepository.findByResponsibleUserId(acadUser.getId())).thenReturn(List.of(academia));
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.desativar(20L, null, auth(acadUser.getEmail()));
        assertThat(resp.getAtivo()).isFalse();
    }

    @Test
    void gerenteAtualizaPersonalDaPropriaAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal p = personal(20L, user(2L, UserRole.PERSONAL), academia);

        when(userRepository.findByEmail(acadUser.getEmail())).thenReturn(Optional.of(acadUser));
        when(academiaContextService.resolveOptional(auth(acadUser.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(p));
        when(academiaRepository.findByResponsibleUserId(acadUser.getId())).thenReturn(List.of(academia));
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new repz.app.dto.request.PersonalUpdateRequest("Pilates", true);
        var resp = service.atualizar(20L, req, null, auth(acadUser.getEmail()));
        assertThat(resp.getEspecialidade()).isEqualTo("Pilates");
    }

    // ─── criar: usuário com role inválida ─────────────────────────────────────

    @Test
    void criarLancaExcecaoQuandoUsuarioNaoTemRolePersonal() {
        User admin = user(1L, UserRole.ADMIN);
        User alunoUser = user(5L, UserRole.ALUNO); // wrong role
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);

        PersonalCreateRequest request = new PersonalCreateRequest(alunoUser.getId(), academia.getId(), "Musculação");
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveRequired(any(), any())).thenReturn(academia.getId());
        when(userRepository.findById(alunoUser.getId())).thenReturn(Optional.of(alunoUser));
        when(mensagens.get(any())).thenReturn("role invalida");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.criar(request, null, auth(admin.getEmail())));
    }

    // ─── gerente tenta alterar personal de outra academia ─────────────────────

    @Test
    void gerenteNaoPodeAlterarStatusPersonalDeOutraAcademia() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User outraAcadUser = user(4L, UserRole.GERENTE);
        Academia outraAcademia = academia(20L, outraAcadUser);
        Personal p = personal(30L, user(2L, UserRole.PERSONAL), outraAcademia);

        when(userRepository.findByEmail(acadUser.getEmail())).thenReturn(Optional.of(acadUser));
        when(academiaContextService.resolveOptional(auth(acadUser.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(30L)).thenReturn(Optional.of(p));
        when(academiaRepository.findByResponsibleUserId(acadUser.getId())).thenReturn(List.of(academia));
        when(mensagens.get(any())).thenReturn("nao pode alterar de outra academia");

        assertThrows(RuntimeException.class, () -> service.ativar(30L, null, auth(acadUser.getEmail())));
    }

    @Test
    void alunoNaoPodeAlterarStatusPersonal() {
        User aluno = user(5L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal p = personal(20L, user(2L, UserRole.PERSONAL), academia);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(academiaContextService.resolveOptional(auth(aluno.getEmail()), null)).thenReturn(null);
        when(personalRepository.findById(20L)).thenReturn(Optional.of(p));
        when(mensagens.get(any())).thenReturn("acesso negado");

        assertThrows(RuntimeException.class, () -> service.ativar(20L, null, auth(aluno.getEmail())));
    }

    // ─── obterMeusAlunos ─────────────────────────────────────────────────────

    @Test
    void personalObtemListaDeSeusAlunos() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        User alunoUser = user(50L, UserRole.ALUNO);
        Aluno aluno = new Aluno();
        aluno.setId(1L);
        aluno.setUsuario(alunoUser);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(alunoRepository.findByPersonalId(personal.getId())).thenReturn(List.of(aluno));

        PersonalAlunosResponse response = service.obterMeusAlunos(auth(personalUser.getEmail()));
        assertThat(response.getPersonalId()).isEqualTo(20L);
        assertThat(response.getAlunos()).hasSize(1);
        assertThat(response.getAlunos().get(0).getNome()).isEqualTo(alunoUser.getName());
    }
}
