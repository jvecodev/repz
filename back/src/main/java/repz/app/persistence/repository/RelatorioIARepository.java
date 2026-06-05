package repz.app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import repz.app.persistence.entity.RelatorioIA;

import java.util.List;

public interface RelatorioIARepository extends JpaRepository<RelatorioIA, Long> {
    List<RelatorioIA> findByAluno_IdOrderByCriadoEmDesc(Long alunoId);
}
