package ar.portal.autoridadmesa.postulante;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PostulanteController {

    private final PostulanteService servicio;

    public PostulanteController(PostulanteService servicio) { this.servicio = servicio; }

    /** Público: lista para el selector del formulario. */
    @GetMapping("/distritos")
    public List<String> distritos() { return Distritos.TODOS; }

    /** Público (RF-05). No devuelve datos personales, solo la confirmación. */
    @PostMapping("/postulantes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrar(@Valid @RequestBody PostulanteRequest request) {
        Postulante p = servicio.registrar(request);
        return Map.of("id", p.getId(), "estado", p.getEstado());
    }

    // --- Solo administrador ---

    @GetMapping("/postulantes")
    public List<PostulanteDto> consultar() {
        return servicio.consultar().stream().map(PostulanteDto::desde).toList();
    }

    @GetMapping("/postulantes/{id}")
    public PostulanteDto obtener(@PathVariable Long id) { return PostulanteDto.desde(servicio.obtener(id)); }

    @PostMapping("/postulantes/{id}/aprobar")
    public PostulanteDto aprobar(@PathVariable Long id) { return PostulanteDto.desde(servicio.aprobar(id)); }

    @PostMapping("/postulantes/{id}/rechazar")
    public PostulanteDto rechazar(@PathVariable Long id, @RequestBody Map<String, String> cuerpo) {
        return PostulanteDto.desde(servicio.rechazar(id, cuerpo.get("motivo")));
    }
}
