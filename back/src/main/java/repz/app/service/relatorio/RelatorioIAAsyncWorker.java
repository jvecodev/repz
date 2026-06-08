package repz.app.service.relatorio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import repz.app.persistence.entity.AvaliacaoFisica;
import repz.app.persistence.entity.RelatorioIA;
import repz.app.persistence.entity.RelatorioStatus;
import repz.app.persistence.repository.AvaliacaoFisicaRepository;
import repz.app.persistence.repository.RelatorioIARepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioIAAsyncWorker {

    private final RelatorioIARepository relatorioRepository;
    private final AvaliacaoFisicaRepository avaliacaoRepository;
    private final AiServiceClient aiServiceClient;

    @Async("relatorioExecutor")
    public void gerarAsync(Long relatorioId, Long alunoId, String alunoNome) {
        log.info("Iniciando geração do relatório {} para aluno {}", relatorioId, alunoId);
        try {
            List<AvaliacaoFisica> avaliacoes =
                    avaliacaoRepository.findByAluno_IdOrderByDataAvaliacaoAsc(alunoId);
            log.info("Relatório {}: {} avaliações físicas encontradas", relatorioId, avaliacoes.size());

            RelatorioIA relatorio = relatorioRepository.findById(relatorioId).orElse(null);
            if (relatorio == null || relatorio.getStatus() == RelatorioStatus.CANCELADO) {
                log.info("Relatório {} cancelado ou não encontrado, abortando geração", relatorioId);
                return;
            }

            String prompt = buildPrompt(alunoNome, avaliacoes);
            log.info("Relatório {}: enviando prompt ao AI service ({} chars)", relatorioId, prompt.length());
            String conteudo = aiServiceClient.gerarRelatorio(prompt);
            log.info("Relatório {}: conteúdo recebido ({} chars)", relatorioId, conteudo != null ? conteudo.length() : 0);

            if (relatorio.getStatus() == RelatorioStatus.PENDENTE) {
                relatorio.setStatus(RelatorioStatus.CONCLUIDO);
                relatorio.setConteudo(conteudo);
                relatorio.setAtualizadoEm(LocalDateTime.now());
                relatorioRepository.save(relatorio);
                log.info("Relatório {} salvo com status CONCLUIDO", relatorioId);
            }

        } catch (Exception e) {
            log.error("Erro ao gerar relatório {}: {}", relatorioId, e.getMessage(), e);
            relatorioRepository.findById(relatorioId).ifPresent(r -> {
                if (r.getStatus() == RelatorioStatus.PENDENTE) {
                    r.setStatus(RelatorioStatus.ERRO);
                    r.setAtualizadoEm(LocalDateTime.now());
                    relatorioRepository.save(r);
                }
            });
        }
    }

    private String buildPrompt(String nomeAluno, List<AvaliacaoFisica> avaliacoes) {
        if (avaliacoes.isEmpty()) {
            return "Aluno " + nomeAluno + " não possui avaliações físicas. Oriente-o a registrar a primeira.";
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy");
        StringBuilder sb = new StringBuilder();
        sb.append("Aluno: ").append(nomeAluno).append("\n\n");
        sb.append("Data | Peso(kg) | Alt(cm) | IMC | Gord% | Cin(cm) | Qua(cm) | Bra(cm) | Cox(cm)\n");
        sb.append("-----|----------|---------|-----|-------|---------|---------|---------|--------\n");

        for (AvaliacaoFisica av : avaliacoes) {
            sb.append(av.getDataAvaliacao() != null ? av.getDataAvaliacao().format(fmt) : "?").append(" | ");
            sb.append(av.getPesoKg() != null            ? av.getPesoKg()                          : "-").append(" | ");
            sb.append(av.getAlturaCm() != null          ? av.getAlturaCm()                        : "-").append(" | ");
            sb.append(av.getImc() != null               ? String.format("%.1f", av.getImc())      : "-").append(" | ");
            sb.append(av.getPercentualGordura() != null ? av.getPercentualGordura()                : "-").append(" | ");
            sb.append(av.getCinturaCm() != null         ? av.getCinturaCm()                       : "-").append(" | ");
            sb.append(av.getQuadrilCm() != null         ? av.getQuadrilCm()                       : "-").append(" | ");
            sb.append(av.getBracoCm() != null           ? av.getBracoCm()                         : "-").append(" | ");
            sb.append(av.getCoxaCm() != null            ? av.getCoxaCm()                          : "-").append("\n");
        }

        sb.append("\nEscreva um relatório motivador e objetivo em português: resumo da evolução, conquistas, pontos de atenção e próximos passos. Máximo 3 parágrafos curtos.");
        return sb.toString();
    }
}
