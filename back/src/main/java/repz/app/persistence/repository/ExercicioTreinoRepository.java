package repz.app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import repz.app.persistence.entity.ExercicioTreino;

import java.util.List;

@Repository
public interface ExercicioTreinoRepository extends JpaRepository<ExercicioTreino, Long> {

    List<ExercicioTreino> findByTreino_IdOrderByOrdemAsc(Long treinoId);
}
