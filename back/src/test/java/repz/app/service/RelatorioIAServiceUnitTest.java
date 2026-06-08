package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.persistence.entity.RelatorioIA;
import repz.app.persistence.entity.RelatorioStatus;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.RelatorioIARepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.relatorio.RelatorioIAAsyncWorker;
import repz.app.service.relatorio.RelatorioIAService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class RelatorioIAServiceUnitTest {

    @Mock private RelatorioIARepository relatorioRepository;
    @Mock private UserRepository userRepository;
    @Mock private RelatorioIAAsyncWorker asyncWorker;
    @InjectMocks private RelatorioIAService service;

    private RelatorioIA criarRelatorio(Long id, User aluno, RelatorioStatus status) {
        RelatorioIA r = new RelatorioIA();
        r.setId(id);
        r.setAluno(aluno);
        r.setStatus(status);
        r.setCriadoEm(LocalDateTime.now());
        r.setAtualizadoEm(LocalDateTime.now());
        return r;
    }

    // ─── iniciar ──────────────────────────────────────────────────────────────

    @Test
    void alunoIniciaPropriRelatorio() {
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA salvo = criarRelatorio(1L, aluno, RelatorioStatus.PENDENTE);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(userRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(relatorioRepository.save(any())).thenReturn(salvo);

        var resp = service.iniciar(aluno.getId(), auth(aluno.getEmail()));

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getStatus()).isEqualTo(RelatorioStatus.PENDENTE);
        verify(asyncWorker).gerarAsync(1L, aluno.getId(), aluno.getName());
    }

    @Test
    void alunoNaoPodeIniciarRelatorioDeOutro() {
        User aluno = user(10L, UserRole.ALUNO);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));

        assertThrows(RuntimeException.class,
                () -> service.iniciar(99L, auth(aluno.getEmail())));
    }

    @Test
    void personalPodeIniciarRelatorioDeQualquerAluno() {
        User personalUser = user(5L, UserRole.PERSONAL);
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA salvo = criarRelatorio(1L, aluno, RelatorioStatus.PENDENTE);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(userRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(relatorioRepository.save(any())).thenReturn(salvo);

        var resp = service.iniciar(aluno.getId(), auth(personalUser.getEmail()));
        assertThat(resp.getStatus()).isEqualTo(RelatorioStatus.PENDENTE);
    }

    // ─── buscar ───────────────────────────────────────────────────────────────

    @Test
    void buscarRetornaRelatorio() {
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA r = criarRelatorio(1L, aluno, RelatorioStatus.CONCLUIDO);
        r.setConteudo("Análise de evolução...");

        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(r));

        var resp = service.buscar(1L);
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getStatus()).isEqualTo(RelatorioStatus.CONCLUIDO);
    }

    @Test
    void buscarLancaExcecaoQuandoNaoExiste() {
        when(relatorioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.buscar(99L));
    }

    // ─── listar ───────────────────────────────────────────────────────────────

    @Test
    void listarRetornaRelatoriosDoAluno() {
        User aluno = user(10L, UserRole.ALUNO);
        var lista = List.of(
                criarRelatorio(1L, aluno, RelatorioStatus.CONCLUIDO),
                criarRelatorio(2L, aluno, RelatorioStatus.PENDENTE));

        when(relatorioRepository.findByAluno_IdOrderByCriadoEmDesc(aluno.getId())).thenReturn(lista);

        var result = service.listar(aluno.getId());
        assertThat(result).hasSize(2);
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void personalAtualizaConteudoDoRelatorio() {
        User personalUser = user(5L, UserRole.PERSONAL);
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA r = criarRelatorio(1L, aluno, RelatorioStatus.PENDENTE);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(r));
        when(relatorioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.atualizar(1L, "Novo conteúdo", auth(personalUser.getEmail()));
        assertThat(resp.getStatus()).isEqualTo(RelatorioStatus.CONCLUIDO);
    }

    @Test
    void alunoNaoPodeAtualizarRelatorioDeOutro() {
        User aluno = user(10L, UserRole.ALUNO);
        User outroAluno = user(11L, UserRole.ALUNO);
        RelatorioIA r = criarRelatorio(1L, outroAluno, RelatorioStatus.PENDENTE);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(r));

        assertThrows(RuntimeException.class,
                () -> service.atualizar(1L, "conteúdo", auth(aluno.getEmail())));
    }

    // ─── excluir ──────────────────────────────────────────────────────────────

    @Test
    void excluirRemoveRelatorio() {
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA r = criarRelatorio(1L, aluno, RelatorioStatus.CONCLUIDO);

        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(r));

        service.excluir(1L);
        verify(relatorioRepository).delete(r);
    }

    @Test
    void excluirLancaExcecaoQuandoNaoExiste() {
        when(relatorioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.excluir(99L));
    }
}
