package ar.portal.autoridadmesa.postulante;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Formulario de registro (RF-05). */
public record PostulanteRequest(
        @NotBlank String distritoElectoral,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos, sin puntos") String dni,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Size(max = 200) String domicilio,
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank @Email @Pattern(regexp = ".+@.+\\..+", message = "El correo debe tener formato usuario@dominio") String email,
        @NotNull Boolean antecedentesMesa,
        @NotBlank @Size(max = 100) String estadoCapacitacion,
        @NotNull Boolean afiliado,
        @Size(max = 100) String partido,
        Long charlaInteresId) {}
