package ar.portal.autoridadmesa.charla;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/charlas")
public class CharlaController {

    private final CharlaRepository repo;

    public CharlaController(CharlaRepository repo) { this.repo = repo; }

    /** Público: por defecto solo charlas futuras; ?todas=true incluye las pasadas. */
    @GetMapping
    public List<CharlaDto> listar(@RequestParam(defaultValue = "false") boolean todas) {
        List<Charla> charlas = todas
                ? repo.findAllByOrderByFechaHoraDesc()
                : repo.findByFechaHoraGreaterThanEqualOrderByFechaHoraAsc(LocalDateTime.now());
        return charlas.stream().map(CharlaDto::desde).toList();
    }

    @GetMapping("/{id}")
    public CharlaDto obtener(@PathVariable Long id) {
        return CharlaDto.desde(buscar(id));
    }

    // --- Solo administradores (ver SecurityConfig) ---

    @PostMapping
    public ResponseEntity<CharlaDto> crear(@Valid @RequestBody CharlaDto dto) {
        Charla c = new Charla();
        dto.copiarA(c);
        c = repo.save(c);
        return ResponseEntity.created(URI.create("/api/charlas/" + c.getId())).body(CharlaDto.desde(c));
    }

    @PutMapping("/{id}")
    public CharlaDto actualizar(@PathVariable Long id, @Valid @RequestBody CharlaDto dto) {
        Charla c = buscar(id);
        dto.copiarA(c);
        return CharlaDto.desde(repo.save(c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        repo.delete(buscar(id));
        return ResponseEntity.noContent().build();
    }

    private Charla buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charla no encontrada"));
    }
}
