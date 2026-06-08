package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import repz.app.persistence.entity.AvaliacaoFisica;
import repz.app.persistence.entity.RelatorioIA;
import repz.app.persistence.entity.RelatorioStatus;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AvaliacaoFisicaRepository;
import repz.app.persistence.repository.RelatorioIARepository;
import repz.app.service.relatorio.AiServiceClient;
import repz.app.service.relatorio.RelatorioIAAsyncWorker;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelatorioIAAsyncWorkerUnitTest {

    @Mock private RelatorioIARepository relatorioRepository;
    @Mock private AvaliacaoFisicaRepository avaliacaoRepository;
    @Mock private AiServiceClient aiServiceClient;

    @InjectMocks private RelatorioIAAsyncWorker worker;

    private RelatorioIA criarRelatorio(Long id, User aluno, RelatorioStatus status) {
        RelatorioIA r = new RelatorioIA();
        r.setId(id);
        r.setAluno(aluno);
        r.setStatus(status);
        r.setCriadoEm(LocalDateTime.now());
        r.setAtualizadoEm(LocalDateTime.now());
        return r;
    }

    @Test
    void gerarAsyncConcluidoComSucesso() {
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA relatorio = criarRelatorio(1L, aluno, RelatorioStatus.PENDENTE);

        when(avaliacaoRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .thenReturn(List.of());
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(relatorio));
        when(aiServiceClient.gerarRelatorio(any())).thenReturn("Relatório gerado com sucesso.");
        when(relatorioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.gerarAsync(1L, aluno.getId(), aluno.getName());

        assertThat(relatorio.getStatus()).isEqualTo(RelatorioStatus.CONCLUIDO);
        assertThat(relatorio.getConteudo()).isEqualTo("Relatório gerado com sucesso.");
        verify(relatorioRepository).save(relatorio);
    }

    @Test
    void gerarAsyncAbortaQuandoRelatorioNaoEncontrado() {
        User aluno = user(10L, UserRole.ALUNO);

        when(avaliacaoRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .thenReturn(List.of());
        when(relatorioRepository.findById(99L)).thenReturn(Optional.empty());

        worker.gerarAsync(99L, aluno.getId(), aluno.getName());

        verify(aiServiceClient, never()).gerarRelatorio(any());
        verify(relatorioRepository, never()).save(any());
    }

    @Test
    void gerarAsyncAbortaQuandoRelatorioCancelado() {
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA relatorio = criarRelatorio(1L, aluno, RelatorioStatus.CANCELADO);

        when(avaliacaoRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .thenReturn(List.of());
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(relatorio));

        worker.gerarAsync(1L, aluno.getId(), aluno.getName());

        verify(aiServiceClient, never()).gerarRelatorio(any());
        verify(relatorioRepository, never()).save(any());
    }

    @Test
    void gerarAsyncMarcaErroQuandoAiServiceFalha() {
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA relatorio = criarRelatorio(1L, aluno, RelatorioStatus.PENDENTE);

        when(avaliacaoRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .thenReturn(List.of());
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(relatorio));
        when(aiServiceClient.gerarRelatorio(any())).thenThrow(new RuntimeException("AI service down"));
        when(relatorioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.gerarAsync(1L, aluno.getId(), aluno.getName());

        assertThat(relatorio.getStatus()).isEqualTo(RelatorioStatus.ERRO);
        verify(relatorioRepository).save(relatorio);
    }

    @Test
    void gerarAsyncComAvaliacoesCriaPropmt() {
        User aluno = user(10L, UserRole.ALUNO);
        RelatorioIA relatorio = criarRelatorio(1L, aluno, RelatorioStatus.PENDENTE);

        AvaliacaoFisica av = new AvaliacaoFisica();
        av.setDataAvaliacao(LocalDateTime.now());
        av.setPesoKg(75.0);
        av.setAlturaCm(175.0);
        av.setImc(24.5);

        when(avaliacaoRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .thenReturn(List.of(av));
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(relatorio));
        when(aiServiceClient.gerarRelatorio(any())).thenReturn("Evolução excelente.");
        when(relatorioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.gerarAsync(1L, aluno.getId(), aluno.getName());

        assertThat(relatorio.getStatus()).isEqualTo(RelatorioStatus.CONCLUIDO);
        assertThat(relatorio.getConteudo()).isEqualTo("Evolução excelente.");
    }
}
