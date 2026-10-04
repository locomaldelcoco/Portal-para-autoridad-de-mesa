package ar.portal.autoridadmesa.inscripcion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByDni(String dni);
    List<Inscripcion> findAllByOrderByFechaInscripcionDesc();
}
