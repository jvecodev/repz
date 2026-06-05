package repz.app.service.relatorio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.response.RelatorioIAResponse;
import repz.app.persistence.entity.AvaliacaoFisica;
import repz.app.persistence.entity.RelatorioIA;
import repz.app.persistence.entity.RelatorioStatus;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AvaliacaoFisicaRepository;
import repz.app.persistence.repository.RelatorioIARepository;
import repz.app.persistence.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioIAService {

    private final RelatorioIARepository relatorioRepository;
    private final AvaliacaoFisicaRepository avaliacaoRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;

    /** Self-injection via proxy para que @Async funcione na chamada interna. */
    @Autowired @Lazy
    private RelatorioIAService self;

    @Transactional
    public RelatorioIAResponse iniciar(Long alunoId, Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Aluno só pode gerar seu próprio relatório; personal pode gerar de qualquer aluno seu
        if (currentUser.getRole() == UserRole.ALUNO && !currentUser.getId().equals(alunoId)) {
            throw new RuntimeException("Acesso negado.");
        }

        User aluno = userRepository.findById(Math.toIntExact(alunoId))
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

        RelatorioIA relatorio = new RelatorioIA();
        relatorio.setAluno(aluno);
        relatorio.setStatus(RelatorioStatus.PENDENTE);
        relatorio.setCriadoEm(LocalDateTime.now());
        relatorio.setAtualizadoEm(LocalDateTime.now());
        RelatorioIA saved = relatorioRepository.save(relatorio);

        self.gerarAsync(saved.getId(), alunoId, aluno.getName());

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public RelatorioIAResponse buscar(Long id) {
        RelatorioIA relatorio = relatorioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));
        return toDTO(relatorio);
    }

    @Transactional(readOnly = true)
    public List<RelatorioIAResponse> listar(Long alunoId) {
        return relatorioRepository.findByAluno_IdOrderByCriadoEmDesc(alunoId)
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public RelatorioIAResponse atualizar(Long id, String conteudo, Authentication auth) {
        RelatorioIA relatorio = relatorioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));

        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (currentUser.getRole() == UserRole.ALUNO
                && !relatorio.getAluno().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Acesso negado.");
        }

        relatorio.setConteudo(conteudo);
        relatorio.setStatus(RelatorioStatus.CONCLUIDO);
        relatorio.setAtualizadoEm(LocalDateTime.now());
        return toDTO(relatorioRepository.save(relatorio));
    }

    @Transactional
    public void excluir(Long id) {
        RelatorioIA relatorio = relatorioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado."));
        relatorioRepository.delete(relatorio);
    }

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

            relatorio = relatorioRepository.findById(relatorioId).orElse(null);
            if (relatorio != null && relatorio.getStatus() == RelatorioStatus.PENDENTE) {
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
            sb.append(av.getPesoKg() != null           ? av.getPesoKg()                          : "-").append(" | ");
            sb.append(av.getAlturaCm() != null         ? av.getAlturaCm()                        : "-").append(" | ");
            sb.append(av.getImc() != null              ? String.format("%.1f", av.getImc())      : "-").append(" | ");
            sb.append(av.getPercentualGordura() != null? av.getPercentualGordura()                : "-").append(" | ");
            sb.append(av.getCinturaCm() != null        ? av.getCinturaCm()                       : "-").append(" | ");
            sb.append(av.getQuadrilCm() != null        ? av.getQuadrilCm()                       : "-").append(" | ");
            sb.append(av.getBracoCm() != null          ? av.getBracoCm()                         : "-").append(" | ");
            sb.append(av.getCoxaCm() != null           ? av.getCoxaCm()                          : "-").append("\n");
        }

        sb.append("\nEscreva um relatório motivador e objetivo em português: resumo da evolução, conquistas, pontos de atenção e próximos passos. Máximo 3 parágrafos curtos.");
        return sb.toString();
    }

    private RelatorioIAResponse toDTO(RelatorioIA r) {
        return new RelatorioIAResponse(r.getId(), r.getStatus(), r.getConteudo(), r.getCriadoEm());
    }
}
