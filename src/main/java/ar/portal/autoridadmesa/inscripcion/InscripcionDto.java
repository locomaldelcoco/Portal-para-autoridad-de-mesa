package ar.portal.autoridadmesa.inscripcion;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Formulario de inscripción. Las validaciones corren en el backend aunque el frontend también valide. */
public record InscripcionDto(
        Long id,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos, sin puntos") String dni,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank String domicilio,
        @NotBlank String localidad,
        @AssertTrue(message = "Debe aceptar los términos") boolean aceptaTerminos,
        LocalDateTime fechaInscripcion) {

    static InscripcionDto desde(Inscripcion i) {
        return new InscripcionDto(i.getId(), i.getNombre(), i.getApellido(), i.getDni(),
                i.getFechaNacimiento(), i.getEmail(), i.getTelefono(), i.getDomicilio(),
                i.getLocalidad(), i.isAceptaTerminos(), i.getFechaInscripcion());
    }
}
