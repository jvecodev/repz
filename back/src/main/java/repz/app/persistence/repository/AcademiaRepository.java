package repz.app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import repz.app.persistence.entity.Academia;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademiaRepository extends JpaRepository<Academia, Long> {
    Optional<Academia> findByCnpj(String cnpj);

    List<Academia> findByActiveTrue();

    Optional<Academia> findByIdAndActiveTrue(Long id);

    List<Academia> findByResponsibleUserId(Long userId);

    @Query(value = "SELECT * FROM fn_dashboard_academia()", nativeQuery = true)
    List<Object[]> dashboard();
}
