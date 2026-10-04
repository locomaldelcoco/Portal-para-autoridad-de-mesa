package ar.portal.autoridadmesa.charla;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CharlaDto(
        Long id,
        @NotBlank String nombre,
        @NotBlank String tema,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horario,
        @NotBlank String sedeNombre,
        @NotBlank String sedeDireccion) {

    static CharlaDto desde(Charla c) {
        return new CharlaDto(c.getId(), c.getNombre(), c.getTema(), c.getFecha(), c.getHorario(),
                c.getSedeNombre(), c.getSedeDireccion());
    }
}
