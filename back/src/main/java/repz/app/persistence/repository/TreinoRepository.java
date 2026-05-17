package repz.app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import repz.app.persistence.entity.Treino;

import java.util.List;

@Repository
public interface TreinoRepository extends JpaRepository<Treino, Long> {

    List<Treino> findByAluno_IdAndAtivoTrueOrderByDivisaoAsc(Long alunoId);

    List<Treino> findByAluno_IdAndAtivoFalseOrderByDtInclusaoDesc(Long alunoId);

    List<Treino> findByAluno_IdOrderByDivisaoAsc(Long alunoId);
}
