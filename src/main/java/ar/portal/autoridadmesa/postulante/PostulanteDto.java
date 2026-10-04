package ar.portal.autoridadmesa.postulante;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Vista de un postulante para el administrador. */
public record PostulanteDto(
        Long id, String distritoElectoral, String nombre, String apellido, String dni, LocalDate fechaNacimiento,
        String domicilio, String telefono, String email, boolean antecedentesMesa, String estadoCapacitacion,
        boolean afiliado, String partido, Long charlaInteresId, EstadoPostulante estado, String motivoRechazo,
        LocalDateTime fechaRegistro) {

    static PostulanteDto desde(Postulante p) {
        return new PostulanteDto(p.getId(), p.getDistritoElectoral(), p.getNombre(), p.getApellido(), p.getDni(),
                p.getFechaNacimiento(), p.getDomicilio(), p.getTelefono(), p.getEmail(), p.isAntecedentesMesa(),
                p.getEstadoCapacitacion(), p.isAfiliado(), p.getPartido(), p.getCharlaInteresId(), p.getEstado(),
                p.getMotivoRechazo(), p.getFechaRegistro());
    }
}
