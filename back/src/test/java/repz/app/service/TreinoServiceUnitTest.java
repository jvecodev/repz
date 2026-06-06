package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import repz.app.dto.request.ExercicioTreinoCreateRequest;
import repz.app.dto.request.TreinoCreateRequest;
import repz.app.dto.response.TreinoResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.Treino;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.TreinoRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.treino.TreinoServiceImpl;

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
class TreinoServiceUnitTest {

    @Mock
    private TreinoRepository treinoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private TreinoServiceImpl service;

    // ─── criar ───────────────────────────────────────────────────────────────

    @Test
    void personalCriaTreinoComExercicios() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User aluno = user(2L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, personalUser, academia);

        var req = new TreinoCreateRequest();
        req.setAlunoId(aluno.getId());
        req.setNome("Treino A");
        req.setDivisao("A");
        req.setObjetivo("Hipertrofia");

        var ex1 = new ExercicioTreinoCreateRequest();
        ex1.setNomeExercicio("Supino");
        ex1.setGrupoMuscular("Peito");
        ex1.setSeries(4);
        ex1.setRepeticoes("8-10");

        var ex2 = new ExercicioTreinoCreateRequest();
        ex2.setNomeExercicio("Rosca direta");
        ex2.setGrupoMuscular("Biceps");
        ex2.setSeries(3);

        req.setExercicios(List.of(ex1, ex2));

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(userRepository.findById(Math.toIntExact(aluno.getId()))).thenReturn(Optional.of(aluno));
        when(treinoRepository.save(any(Treino.class))).thenAnswer(inv -> {
            Treino t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        TreinoResponse response = service.criar(req, auth(personalUser.getEmail()));

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getNome()).isEqualTo("Treino A");
        assertThat(response.getDivisao()).isEqualTo("A");
        assertThat(response.getExercicios()).hasSize(2);
        assertThat(response.getExercicios().get(0).getNomeExercicio()).isEqualTo("Supino");

        ArgumentCaptor<Treino> captor = ArgumentCaptor.forClass(Treino.class);
        verify(treinoRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isTrue();
        assertThat(captor.getValue().getExercicios()).hasSize(2);
    }

    @Test
    void alunoNaoPodeCriarTreino() {
        User aluno = user(2L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));

        var req = new TreinoCreateRequest();
        req.setAlunoId(2L);
        req.setNome("Treino A");
        req.setDivisao("A");

        assertThrows(AccessDeniedException.class, () -> service.criar(req, auth(aluno.getEmail())));
        verify(treinoRepository, never()).save(any());
    }

    @Test
    void adminNaoPodeCriarTreino() {
        User admin = user(1L, UserRole.ADMIN);
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        var req = new TreinoCreateRequest();
        req.setAlunoId(2L);
        req.setNome("Treino A");
        req.setDivisao("A");

        assertThrows(AccessDeniedException.class, () -> service.criar(req, auth(admin.getEmail())));
    }

    @Test
    void criarTreinoFalhaQuandoAlunoNaoExiste() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("not found");

        var req = new TreinoCreateRequest();
        req.setAlunoId(99L);
        req.setNome("Treino A");
        req.setDivisao("A");

        assertThrows(RuntimeException.class, () -> service.criar(req, auth(personalUser.getEmail())));
    }

    @Test
    void criarTreinoFalhaQuandoTargetNaoEhAluno() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User outroPersonal = user(5L, UserRole.PERSONAL);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(userRepository.findById(Math.toIntExact(outroPersonal.getId()))).thenReturn(Optional.of(outroPersonal));
        when(mensagens.get(any())).thenReturn("role invalida");

        var req = new TreinoCreateRequest();
        req.setAlunoId(outroPersonal.getId());
        req.setNome("Treino A");
        req.setDivisao("A");

        assertThrows(RuntimeException.class, () -> service.criar(req, auth(personalUser.getEmail())));
    }

    @Test
    void criarTreinoSemExerciciosRetornaListaVazia() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User aluno = user(2L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, personalUser, academia);

        var req = new TreinoCreateRequest();
        req.setAlunoId(aluno.getId());
        req.setNome("Treino Vazio");
        req.setDivisao("A");
        req.setExercicios(null);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(userRepository.findById(Math.toIntExact(aluno.getId()))).thenReturn(Optional.of(aluno));
        when(treinoRepository.save(any(Treino.class))).thenAnswer(inv -> {
            Treino t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TreinoResponse response = service.criar(req, auth(personalUser.getEmail()));
        assertThat(response.getExercicios()).isEmpty();
    }

    // ─── obterMinhaFichaAtiva ─────────────────────────────────────────────

    @Test
    void alunoObtemPropriaFichaAtiva() {
        User aluno = user(2L, UserRole.ALUNO);
        Treino treino = criarTreino(1L, aluno, null, true);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(treinoRepository.findByAluno_IdAndAtivoTrueOrderByDivisaoAsc(aluno.getId()))
                .thenReturn(List.of(treino));

        List<TreinoResponse> result = service.obterMinhaFichaAtiva(auth(aluno.getEmail()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAlunoId()).isEqualTo(aluno.getId());
    }

    @Test
    void alunoSemFichaRetornaListaVazia() {
        User aluno = user(2L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(treinoRepository.findByAluno_IdAndAtivoTrueOrderByDivisaoAsc(aluno.getId()))
                .thenReturn(List.of());

        List<TreinoResponse> result = service.obterMinhaFichaAtiva(auth(aluno.getEmail()));
        assertThat(result).isEmpty();
    }

    // ─── obterMeuHistorico ────────────────────────────────────────────────

    @Test
    void alunoObtemHistoricoFichasInativas() {
        User aluno = user(2L, UserRole.ALUNO);
        Treino inativo = criarTreino(2L, aluno, null, false);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(treinoRepository.findByAluno_IdAndAtivoFalseOrderByDtInclusaoDesc(aluno.getId()))
                .thenReturn(List.of(inativo));

        List<TreinoResponse> result = service.obterMeuHistorico(auth(aluno.getEmail()));
        assertThat(result).hasSize(1);
    }

    // ─── obterFichaAtivaDoAluno ───────────────────────────────────────────

    @Test
    void personalObtemFichaAtivaDeQualquerAluno() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User aluno = user(2L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, personalUser, academia);
        Treino treino = criarTreino(1L, aluno, personal, true);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(treinoRepository.findByAluno_IdAndAtivoTrueOrderByDivisaoAsc(aluno.getId()))
                .thenReturn(List.of(treino));

        List<TreinoResponse> result = service.obterFichaAtivaDoAluno(aluno.getId(), auth(personalUser.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void alunoObtemPropriaFichaAtivaViaId() {
        User aluno = user(2L, UserRole.ALUNO);
        Treino treino = criarTreino(1L, aluno, null, true);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(treinoRepository.findByAluno_IdAndAtivoTrueOrderByDivisaoAsc(aluno.getId()))
                .thenReturn(List.of(treino));

        List<TreinoResponse> result = service.obterFichaAtivaDoAluno(aluno.getId(), auth(aluno.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void alunoNaoPodeVerFichaDeOutroAluno() {
        User aluno = user(2L, UserRole.ALUNO);
        User outroAluno = user(99L, UserRole.ALUNO);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));

        assertThrows(AccessDeniedException.class,
                () -> service.obterFichaAtivaDoAluno(outroAluno.getId(), auth(aluno.getEmail())));
    }

    // ─── obterHistoricoDoAluno ────────────────────────────────────────────

    @Test
    void personalObtemHistoricoDeAluno() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User aluno = user(2L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(treinoRepository.findByAluno_IdAndAtivoFalseOrderByDtInclusaoDesc(aluno.getId()))
                .thenReturn(List.of());

        List<TreinoResponse> result = service.obterHistoricoDoAluno(aluno.getId(), auth(personalUser.getEmail()));
        assertThat(result).isEmpty();
    }

    // ─── findById ─────────────────────────────────────────────────────────

    @Test
    void alunoObtemProprioTreinoPorId() {
        User aluno = user(2L, UserRole.ALUNO);
        Treino treino = criarTreino(5L, aluno, null, true);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(treinoRepository.findById(5L)).thenReturn(Optional.of(treino));

        TreinoResponse result = service.findById(5L, auth(aluno.getEmail()));
        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void alunoNaoPodeVerTreinoDeOutroAlunoPorId() {
        User aluno = user(2L, UserRole.ALUNO);
        User outroAluno = user(99L, UserRole.ALUNO);
        Treino treino = criarTreino(5L, outroAluno, null, true);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(treinoRepository.findById(5L)).thenReturn(Optional.of(treino));

        assertThrows(AccessDeniedException.class, () -> service.findById(5L, auth(aluno.getEmail())));
    }

    @Test
    void personalPodeVerQualquerTreinoPorId() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User aluno = user(2L, UserRole.ALUNO);
        Treino treino = criarTreino(5L, aluno, null, true);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(treinoRepository.findById(5L)).thenReturn(Optional.of(treino));

        TreinoResponse result = service.findById(5L, auth(personalUser.getEmail()));
        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void findByIdLancaExcecaoQuandoNaoExiste() {
        User admin = user(1L, UserRole.ADMIN);
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(treinoRepository.findById(999L)).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("not found");

        assertThrows(RuntimeException.class, () -> service.findById(999L, auth(admin.getEmail())));
    }

    // ─── ativar / desativar ───────────────────────────────────────────────

    @Test
    void ativarTreinoMudaStatusParaTrue() {
        User aluno = user(2L, UserRole.ALUNO);
        Treino treino = criarTreino(5L, aluno, null, false);

        when(treinoRepository.findById(5L)).thenReturn(Optional.of(treino));
        when(treinoRepository.save(any())).thenReturn(treino);

        service.ativar(5L);

        assertThat(treino.getAtivo()).isTrue();
        verify(treinoRepository).save(treino);
    }

    @Test
    void desativarTreinoMudaStatusParaFalse() {
        User aluno = user(2L, UserRole.ALUNO);
        Treino treino = criarTreino(5L, aluno, null, true);

        when(treinoRepository.findById(5L)).thenReturn(Optional.of(treino));
        when(treinoRepository.save(any())).thenReturn(treino);

        service.desativar(5L);

        assertThat(treino.getAtivo()).isFalse();
        verify(treinoRepository).save(treino);
    }

    @Test
    void ativarTreinoInexistenteLancaExcecao() {
        when(treinoRepository.findById(999L)).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("not found");

        assertThrows(RuntimeException.class, () -> service.ativar(999L));
    }

    // ─── exercícios: auto-ordenação ───────────────────────────────────────

    @Test
    void exerciciosSemOrdemRecebeOrdemAutomatica() {
        User personalUser = user(1L, UserRole.PERSONAL);
        User aluno = user(2L, UserRole.ALUNO);
        User acadUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, acadUser);
        Personal personal = personal(20L, personalUser, academia);

        var ex1 = new ExercicioTreinoCreateRequest();
        ex1.setNomeExercicio("Supino");

        var ex2 = new ExercicioTreinoCreateRequest();
        ex2.setNomeExercicio("Rosca");
        ex2.setOrdem(5); // ordem manual

        var req = new TreinoCreateRequest();
        req.setAlunoId(aluno.getId());
        req.setNome("Treino Auto");
        req.setDivisao("A");
        req.setExercicios(List.of(ex1, ex2));

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));
        when(userRepository.findById(Math.toIntExact(aluno.getId()))).thenReturn(Optional.of(aluno));
        when(treinoRepository.save(any(Treino.class))).thenAnswer(inv -> {
            Treino t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TreinoResponse response = service.criar(req, auth(personalUser.getEmail()));

        assertThat(response.getExercicios().get(0).getOrdem()).isEqualTo(1);   // auto
        assertThat(response.getExercicios().get(1).getOrdem()).isEqualTo(5);   // manual preservado
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private Treino criarTreino(Long id, User aluno, Personal personal, boolean ativo) {
        Treino t = new Treino();
        t.setId(id);
        t.setNome("Treino " + id);
        t.setDivisao("A");
        t.setAtivo(ativo);
        t.setAluno(aluno);
        t.setPersonal(personal);
        t.setExercicios(List.of());
        return t;
    }
}
