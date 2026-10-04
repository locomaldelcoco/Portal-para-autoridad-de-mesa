package ar.portal.autoridadmesa.charla;

import ar.portal.autoridadmesa.config.ValidacionException;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/charlas")
public class CharlaController {

    private final CharlaRepository repo;
    private final Clock reloj;
    private final LocalDate fechaEleccion; // null si no está configurada

    public CharlaController(CharlaRepository repo, Clock reloj,
                            @Value("${portal.eleccion.fecha:}") String fechaEleccion) {
        this.repo = repo;
        this.reloj = reloj;
        this.fechaEleccion = fechaEleccion.isBlank() ? null : LocalDate.parse(fechaEleccion);
    }

    /** RF-02, público: charlas de orientación que todavía no se realizaron. */
    @GetMapping
    public List<CharlaDto> listar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return repo.findAllByOrderByFechaAscHorarioAsc().stream()
                .filter(c -> !c.fechaHora().isBefore(ahora))
                .map(CharlaDto::desde).toList();
    }

    /** RF-01, solo administrador. */
    @PostMapping
    public ResponseEntity<CharlaDto> publicar(@Valid @RequestBody CharlaDto dto) {
        if (LocalDateTime.of(dto.fecha(), dto.horario()).isBefore(LocalDateTime.now(reloj))) {
            throw new ValidacionException("fecha", "La fecha y el horario de la charla ya pasaron");
        }
        if (fechaEleccion != null && !dto.fecha().isBefore(fechaEleccion)) {
            throw new ValidacionException("fecha", "La charla debe ser previa a la elección (" + fechaEleccion + ")");
        }
        Charla c = new Charla();
        c.setNombre(dto.nombre().trim());
        c.setTema(dto.tema().trim());
        c.setFecha(dto.fecha());
        c.setHorario(dto.horario());
        c.setSedeNombre(dto.sedeNombre().trim());
        c.setSedeDireccion(dto.sedeDireccion().trim());
        c = repo.save(c);
        return ResponseEntity.created(URI.create("/api/charlas/" + c.getId())).body(CharlaDto.desde(c));
    }
}
