package repz.app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Plano;

import java.util.List;
import java.util.Optional;

public interface PlanoRepository extends JpaRepository<Plano, Integer> {

    List<Plano> findByAcademia(Academia academia);

    Optional<Plano> findByIdAndAcademia(Integer id, Academia academia);

    List<Plano> findByAcademiaId(Long academiaId);

    Optional<Plano> findByIdAndAcademiaId(Integer id, Long academiaId);
}
