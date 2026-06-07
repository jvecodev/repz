package repz.app.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.response.RelatorioIAResponse;
import repz.app.persistence.entity.RelatorioIA;
import repz.app.persistence.entity.RelatorioStatus;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.RelatorioIARepository;
import repz.app.persistence.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioIAService {

    private final RelatorioIARepository relatorioRepository;
    private final UserRepository userRepository;
    private final RelatorioIAAsyncWorker asyncWorker;

    @Transactional
    public RelatorioIAResponse iniciar(Long alunoId, Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Aluno só pode gerar seu próprio relatório; personal pode gerar de qualquer aluno seu
        if (currentUser.getRole() == UserRole.ALUNO && !currentUser.getId().equals(alunoId)) {
            throw new RuntimeException("Acesso negado.");
        }

        User aluno = userRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

        RelatorioIA relatorio = new RelatorioIA();
        relatorio.setAluno(aluno);
        relatorio.setStatus(RelatorioStatus.PENDENTE);
        relatorio.setCriadoEm(LocalDateTime.now());
        relatorio.setAtualizadoEm(LocalDateTime.now());
        RelatorioIA saved = relatorioRepository.save(relatorio);

        asyncWorker.gerarAsync(saved.getId(), alunoId, aluno.getName());

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

    private RelatorioIAResponse toDTO(RelatorioIA r) {
        return new RelatorioIAResponse(r.getId(), r.getStatus(), r.getConteudo(), r.getCriadoEm());
    }
}
