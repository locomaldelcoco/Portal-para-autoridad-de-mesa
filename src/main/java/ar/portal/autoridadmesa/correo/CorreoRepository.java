package ar.portal.autoridadmesa.correo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorreoRepository extends JpaRepository<CorreoSaliente, Long> {
    List<CorreoSaliente> findByEstadoOrderByIdAsc(CorreoSaliente.Estado estado);
}
