package ar.portal.autoridadmesa.charla;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharlaRepository extends JpaRepository<Charla, Long> {
    List<Charla> findByFechaHoraGreaterThanEqualOrderByFechaHoraAsc(LocalDateTime desde);
    List<Charla> findAllByOrderByFechaHoraDesc();
}
