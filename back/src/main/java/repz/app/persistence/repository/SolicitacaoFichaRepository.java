package repz.app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import repz.app.persistence.entity.SolicitacaoFicha;
import repz.app.persistence.entity.SolicitacaoFichaStatus;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoFichaRepository extends JpaRepository<SolicitacaoFicha, Long> {

    List<SolicitacaoFicha> findByPersonal_IdOrderByCriadaEmDesc(Long personalId);

    List<SolicitacaoFicha> findByPersonal_IdAndStatusOrderByCriadaEmDesc(Long personalId, SolicitacaoFichaStatus status);

    Optional<SolicitacaoFicha> findFirstByAluno_IdAndStatusOrderByCriadaEmDesc(Long alunoId, SolicitacaoFichaStatus status);

    boolean existsByAluno_IdAndStatus(Long alunoId, SolicitacaoFichaStatus status);
}
