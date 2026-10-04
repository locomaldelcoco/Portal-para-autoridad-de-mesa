package ar.portal.autoridadmesa.inscripcion;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private static final int EDAD_MINIMA = 18;

    private final InscripcionRepository repo;

    public InscripcionController(InscripcionRepository repo) { this.repo = repo; }

    /** Público: cualquier ciudadano puede inscribirse. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionDto inscribir(@Valid @RequestBody InscripcionDto dto) {
        if (Period.between(dto.fechaNacimiento(), LocalDate.now()).getYears() < EDAD_MINIMA) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Debe ser mayor de " + EDAD_MINIMA + " años para ser autoridad de mesa");
        }
        if (repo.existsByDni(dto.dni())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una inscripción con ese DNI");
        }
        Inscripcion i = new Inscripcion();
        i.setNombre(dto.nombre().trim());
        i.setApellido(dto.apellido().trim());
        i.setDni(dto.dni());
        i.setFechaNacimiento(dto.fechaNacimiento());
        i.setEmail(dto.email().trim());
        i.setTelefono(dto.telefono().trim());
        i.setDomicilio(dto.domicilio().trim());
        i.setLocalidad(dto.localidad().trim());
        i.setAceptaTerminos(true);
        return InscripcionDto.desde(repo.save(i));
    }

    /** Solo administradores: contiene datos personales. */
    @GetMapping
    public List<InscripcionDto> listar() {
        return repo.findAllByOrderByFechaInscripcionDesc().stream().map(InscripcionDto::desde).toList();
    }
}
