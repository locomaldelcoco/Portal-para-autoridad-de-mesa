package ar.portal.autoridadmesa.postulante;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostulanteRepository extends JpaRepository<Postulante, Long> {
    boolean existsByDniHash(String dniHash);
    boolean existsByEstado(EstadoPostulante estado);
    List<Postulante> findAllByOrderByIdAsc();
    List<Postulante> findByEstadoOrderByIdAsc(EstadoPostulante estado);
}
