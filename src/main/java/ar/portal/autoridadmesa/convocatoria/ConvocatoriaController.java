package ar.portal.autoridadmesa.convocatoria;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convocatoria")
public class ConvocatoriaController {

    private final ConvocatoriaService servicio;

    public ConvocatoriaController(ConvocatoriaService servicio) { this.servicio = servicio; }

    /** Público: el frontend lo usa para decidir si muestra el formulario de inscripción. */
    @GetMapping
    public ConvocatoriaDto estado() { return servicio.estado(); }
}
