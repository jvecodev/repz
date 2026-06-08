package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.dto.request.SolicitacaoFichaCreateRequest;
import repz.app.dto.request.SolicitacaoFichaResponderRequest;
import repz.app.dto.response.SolicitacaoFichaResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.SolicitacaoFicha;
import repz.app.persistence.entity.SolicitacaoFichaStatus;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.SolicitacaoFichaRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.solicitacaoFicha.SolicitacaoFichaServiceImpl;

import java.time.LocalDateTime;
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
class SolicitacaoFichaServiceUnitTest {

    @Mock
    private SolicitacaoFichaRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private SolicitacaoFichaServiceImpl service;

    // ─── criar ───────────────────────────────────────────────────────────────

    @Test
    void alunoCriaSOlicitacaoSemPersonal() {
        User aluno = user(10L, UserRole.ALUNO);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.existsByAluno_IdAndStatus(aluno.getId(), SolicitacaoFichaStatus.PENDENTE)).thenReturn(false);
        when(repository.save(any(SolicitacaoFicha.class))).thenAnswer(inv -> {
            SolicitacaoFicha s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        var req = new SolicitacaoFichaCreateRequest();
        req.setMensagem("Quero foco em hipertrofia");

        SolicitacaoFichaResponse resp = service.criar(req, auth(aluno.getEmail()));

        assertThat(resp.getAlunoId()).isEqualTo(aluno.getId());
        assertThat(resp.getStatus()).isEqualTo(SolicitacaoFichaStatus.PENDENTE);
        assertThat(resp.getMensagem()).isEqualTo("Quero foco em hipertrofia");
    }

    @Test
    void alunoCriaSOlicitacaoComPersonal() {
        User aluno = user(10L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(20L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.existsByAluno_IdAndStatus(aluno.getId(), SolicitacaoFichaStatus.PENDENTE)).thenReturn(false);
        when(personalRepository.findById(personal.getId())).thenReturn(Optional.of(personal));
        when(repository.save(any(SolicitacaoFicha.class))).thenAnswer(inv -> {
            SolicitacaoFicha s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        var req = new SolicitacaoFichaCreateRequest();
        req.setPersonalId(personal.getId());
        req.setMensagem("Quero um treino funcional");

        SolicitacaoFichaResponse resp = service.criar(req, auth(aluno.getEmail()));

        assertThat(resp.getPersonalId()).isEqualTo(personal.getId());
        assertThat(resp.getPersonalNome()).isEqualTo(personalUser.getName());
    }

    @Test
    void criarRejeitaQuandoJaExistePendente() {
        User aluno = user(10L, UserRole.ALUNO);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.existsByAluno_IdAndStatus(aluno.getId(), SolicitacaoFichaStatus.PENDENTE)).thenReturn(true);
        when(mensagens.get(any())).thenReturn("ja pendente");

        var req = new SolicitacaoFichaCreateRequest();
        assertThrows(RuntimeException.class, () -> service.criar(req, auth(aluno.getEmail())));
        verify(repository, never()).save(any());
    }

    @Test
    void criarRejeitaParaNaoAluno() {
        User personal = user(5L, UserRole.PERSONAL);

        when(userRepository.findByEmail(personal.getEmail())).thenReturn(Optional.of(personal));
        when(mensagens.get(any())).thenReturn("apenas aluno");

        var req = new SolicitacaoFichaCreateRequest();
        assertThrows(RuntimeException.class, () -> service.criar(req, auth(personal.getEmail())));
        verify(repository, never()).save(any());
    }

    @Test
    void criarRejeitaQuandoPersonalNaoExiste() {
        User aluno = user(10L, UserRole.ALUNO);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.existsByAluno_IdAndStatus(aluno.getId(), SolicitacaoFichaStatus.PENDENTE)).thenReturn(false);
        when(personalRepository.findById(999L)).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("personal not found");

        var req = new SolicitacaoFichaCreateRequest();
        req.setPersonalId(999L);

        assertThrows(RuntimeException.class, () -> service.criar(req, auth(aluno.getEmail())));
    }

    // ─── cancelar ────────────────────────────────────────────────────────────

    @Test
    void alunoCancelaPropriaSolicitacaoPendente() {
        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, null, SolicitacaoFichaStatus.PENDENTE);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.findById(1L)).thenReturn(Optional.of(sol));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitacaoFichaResponse resp = service.cancelar(1L, auth(aluno.getEmail()));
        assertThat(resp.getStatus()).isEqualTo(SolicitacaoFichaStatus.CANCELADA);
    }

    @Test
    void cancelarRejeitaQuandoNaoEProprioAluno() {
        User aluno = user(10L, UserRole.ALUNO);
        User outroAluno = user(99L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, outroAluno, null, SolicitacaoFichaStatus.PENDENTE);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.findById(1L)).thenReturn(Optional.of(sol));
        when(mensagens.get(any())).thenReturn("acesso negado");

        assertThrows(RuntimeException.class, () -> service.cancelar(1L, auth(aluno.getEmail())));
    }

    @Test
    void cancelarRejeitaQuandoNaoPendente() {
        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, null, SolicitacaoFichaStatus.APROVADA);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.findById(1L)).thenReturn(Optional.of(sol));
        when(mensagens.get(any())).thenReturn("nao pode cancelar");

        assertThrows(RuntimeException.class, () -> service.cancelar(1L, auth(aluno.getEmail())));
    }

    // ─── pendente ────────────────────────────────────────────────────────────

    @Test
    void retornaSolicitacaoPendenteDoAluno() {
        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, null, SolicitacaoFichaStatus.PENDENTE);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.findFirstByAluno_IdAndStatusOrderByCriadaEmDesc(aluno.getId(), SolicitacaoFichaStatus.PENDENTE))
                .thenReturn(Optional.of(sol));

        SolicitacaoFichaResponse resp = service.pendente(auth(aluno.getEmail()));
        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(SolicitacaoFichaStatus.PENDENTE);
    }

    @Test
    void retornaNull_QuandoAlunNaoTempendente() {
        User aluno = user(10L, UserRole.ALUNO);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(repository.findFirstByAluno_IdAndStatusOrderByCriadaEmDesc(aluno.getId(), SolicitacaoFichaStatus.PENDENTE))
                .thenReturn(Optional.empty());

        SolicitacaoFichaResponse resp = service.pendente(auth(aluno.getEmail()));
        assertThat(resp).isNull();
    }

    // ─── listarParaPersonal ───────────────────────────────────────────────────

    @Test
    void personalListaSolicitacoesPendentes() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(20L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);

        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, personal, SolicitacaoFichaStatus.PENDENTE);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(repository.findByPersonal_IdAndStatusOrderByCriadaEmDesc(personal.getId(), SolicitacaoFichaStatus.PENDENTE))
                .thenReturn(List.of(sol));

        List<SolicitacaoFichaResponse> result = service.listarParaPersonal(SolicitacaoFichaStatus.PENDENTE, auth(personalUser.getEmail()));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(SolicitacaoFichaStatus.PENDENTE);
    }

    @Test
    void personalListaTodasSolicitacoesSemFiltroDeStatus() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(20L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);

        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol1 = criarSolicitacao(1L, aluno, personal, SolicitacaoFichaStatus.PENDENTE);
        SolicitacaoFicha sol2 = criarSolicitacao(2L, aluno, personal, SolicitacaoFichaStatus.APROVADA);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(repository.findByPersonal_IdOrderByCriadaEmDesc(personal.getId()))
                .thenReturn(List.of(sol1, sol2));

        List<SolicitacaoFichaResponse> result = service.listarParaPersonal(null, auth(personalUser.getEmail()));
        assertThat(result).hasSize(2);
    }

    @Test
    void listarRejeitaParaNaoPersonal() {
        User aluno = user(10L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(mensagens.get(any())).thenReturn("apenas personal");

        assertThrows(RuntimeException.class,
                () -> service.listarParaPersonal(SolicitacaoFichaStatus.PENDENTE, auth(aluno.getEmail())));
    }

    // ─── responder ────────────────────────────────────────────────────────────

    @Test
    void personalAprovaSOlicitacaoPendente() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(20L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);

        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, personal, SolicitacaoFichaStatus.PENDENTE);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(repository.findById(1L)).thenReturn(Optional.of(sol));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new SolicitacaoFichaResponderRequest();
        req.setStatus(SolicitacaoFichaStatus.APROVADA);
        req.setResposta("Vou preparar a ficha!");

        SolicitacaoFichaResponse resp = service.responder(1L, req, auth(personalUser.getEmail()));
        assertThat(resp.getStatus()).isEqualTo(SolicitacaoFichaStatus.APROVADA);
        assertThat(resp.getResposta()).isEqualTo("Vou preparar a ficha!");
    }

    @Test
    void personalRejeitaSolicitacao() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(20L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);

        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, personal, SolicitacaoFichaStatus.PENDENTE);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(repository.findById(1L)).thenReturn(Optional.of(sol));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new SolicitacaoFichaResponderRequest();
        req.setStatus(SolicitacaoFichaStatus.REJEITADA);

        SolicitacaoFichaResponse resp = service.responder(1L, req, auth(personalUser.getEmail()));
        assertThat(resp.getStatus()).isEqualTo(SolicitacaoFichaStatus.REJEITADA);
    }

    @Test
    void personalVinculaSeSolicitacaoFoiSemPersonal() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(20L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);

        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, null, SolicitacaoFichaStatus.PENDENTE);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(repository.findById(1L)).thenReturn(Optional.of(sol));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new SolicitacaoFichaResponderRequest();
        req.setStatus(SolicitacaoFichaStatus.APROVADA);

        SolicitacaoFichaResponse resp = service.responder(1L, req, auth(personalUser.getEmail()));
        assertThat(resp.getPersonalId()).isEqualTo(personal.getId());
    }

    @Test
    void responderRejeitaQuandoNaoPendente() {
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(20L, acadUser);
        User personalUser = user(5L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);

        User aluno = user(10L, UserRole.ALUNO);
        SolicitacaoFicha sol = criarSolicitacao(1L, aluno, personal, SolicitacaoFichaStatus.APROVADA);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(repository.findById(1L)).thenReturn(Optional.of(sol));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(mensagens.get(any())).thenReturn("ja respondida");

        var req = new SolicitacaoFichaResponderRequest();
        req.setStatus(SolicitacaoFichaStatus.APROVADA);

        assertThrows(RuntimeException.class, () -> service.responder(1L, req, auth(personalUser.getEmail())));
    }

    @Test
    void responderRejeitaParaNaoPersonal() {
        User aluno = user(10L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(mensagens.get(any())).thenReturn("apenas personal");

        var req = new SolicitacaoFichaResponderRequest();
        req.setStatus(SolicitacaoFichaStatus.APROVADA);

        assertThrows(RuntimeException.class, () -> service.responder(1L, req, auth(aluno.getEmail())));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private SolicitacaoFicha criarSolicitacao(Long id, User aluno, Personal personal, SolicitacaoFichaStatus status) {
        SolicitacaoFicha s = new SolicitacaoFicha();
        s.setId(id);
        s.setAluno(aluno);
        s.setPersonal(personal);
        s.setStatus(status);
        s.setMensagem("Mensagem " + id);
        s.setCriadaEm(LocalDateTime.now());
        return s;
    }
}
