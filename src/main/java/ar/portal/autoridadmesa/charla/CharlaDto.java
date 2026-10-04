package ar.portal.autoridadmesa.charla;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** Datos que viajan entre el frontend y el backend (nunca exponemos la entidad directamente). */
public record CharlaDto(
        Long id,
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 2000) String descripcion,
        @NotNull LocalDateTime fechaHora,
        @NotBlank String lugar,
        @NotBlank String direccion,
        @NotBlank String localidad,
        @NotEmpty List<@NotBlank String> temas) {

    static CharlaDto desde(Charla c) {
        return new CharlaDto(c.getId(), c.getTitulo(), c.getDescripcion(), c.getFechaHora(),
                c.getLugar(), c.getDireccion(), c.getLocalidad(), List.copyOf(c.getTemas()));
    }

    void copiarA(Charla c) {
        c.setTitulo(titulo.trim());
        c.setDescripcion(descripcion);
        c.setFechaHora(fechaHora);
        c.setLugar(lugar.trim());
        c.setDireccion(direccion.trim());
        c.setLocalidad(localidad.trim());
        c.setTemas(temas.stream().map(String::trim).toList());
    }
}
