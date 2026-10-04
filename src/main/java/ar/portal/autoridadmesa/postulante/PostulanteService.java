package ar.portal.autoridadmesa.postulante;

import ar.portal.autoridadmesa.charla.CharlaRepository;
import ar.portal.autoridadmesa.config.ValidacionException;
import ar.portal.autoridadmesa.convocatoria.ConvocatoriaService;
import ar.portal.autoridadmesa.convocatoria.EstadoConvocatoria;
import ar.portal.autoridadmesa.correo.CorreoService;
import ar.portal.autoridadmesa.seguridad.CifradoService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostulanteService {

    private final PostulanteRepository repo;
    private final CharlaRepository charlas;
    private final ConvocatoriaService convocatoria;
    private final CorreoService correo;
    private final CifradoService cifrado;
    private final Clock reloj;

    public PostulanteService(PostulanteRepository repo, CharlaRepository charlas, ConvocatoriaService convocatoria,
                             CorreoService correo, CifradoService cifrado, Clock reloj) {
        this.repo = repo;
        this.charlas = charlas;
        this.convocatoria = convocatoria;
        this.correo = correo;
        this.cifrado = cifrado;
        this.reloj = reloj;
    }

    /** RF-05: registro de un ciudadano. Solo con la convocatoria abierta (RF-03 / RF-04). */
    @Transactional
    public Postulante registrar(PostulanteRequest r) {
        if (convocatoria.estado().estado() != EstadoConvocatoria.ABIERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripción no está habilitada en este momento");
        }
        if (!Distritos.TODOS.contains(r.distritoElectoral())) {
            throw new ValidacionException("distritoElectoral", "El distrito electoral no es reconocido");
        }
        if (!telefonoArgentino(r.telefono())) {
            throw new ValidacionException("telefono", "El teléfono debe ser un número de la República Argentina");
        }
        if (r.afiliado() && (r.partido() == null || r.partido().isBlank())) {
            throw new ValidacionException("partido", "Indique el partido al que está afiliado");
        }
        if (r.charlaInteresId() != null && !charlas.existsById(r.charlaInteresId())) {
            throw new ValidacionException("charlaInteresId", "La charla seleccionada no existe");
        }
        String hashDni = cifrado.hash(r.dni());
        if (repo.existsByDniHash(hashDni)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una inscripción con ese DNI");
        }

        Postulante p = new Postulante();
        p.setDistritoElectoral(r.distritoElectoral());
        p.setNombre(r.nombre().trim());
        p.setApellido(r.apellido().trim());
        p.setDni(r.dni());
        p.setDniHash(hashDni);
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setDomicilio(r.domicilio().trim());
        p.setTelefono(r.telefono().trim());
        p.setEmail(r.email().trim());
        p.setAntecedentesMesa(r.antecedentesMesa());
        p.setEstadoCapacitacion(r.estadoCapacitacion().trim());
        p.setAfiliado(r.afiliado());
        p.setPartido(r.afiliado() ? r.partido().trim() : null);
        p.setCharlaInteresId(r.charlaInteresId());
        p.setFechaRegistro(LocalDateTime.now(reloj));
        return repo.save(p);
    }

    /** RF-08 / CU2: solo con la convocatoria cerrada. */
    @Transactional(readOnly = true)
    public List<Postulante> consultar() {
        exigirCerrada();
        return repo.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Postulante obtener(Long id) {
        exigirCerrada();
        return buscar(id);
    }

    /** RF-09 / CU4 + CU6. */
    @Transactional
    public Postulante aprobar(Long id) {
        Postulante p = pendiente(id);
        p.setEstado(EstadoPostulante.ACEPTADO);
        correo.encolar(p.getEmail(), "Su solicitud fue aprobada",
                "Hola " + p.getNombre() + ",\n\nSu solicitud para ser autoridad de mesa fue aprobada.\n");
        convocatoria.enviarReporteFinalSiCorresponde(); // CU8
        return p;
    }

    /** RF-11 / CU5 + CU7. El motivo es obligatorio. */
    @Transactional
    public Postulante rechazar(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ValidacionException("motivo", "El motivo del rechazo es obligatorio");
        }
        Postulante p = pendiente(id);
        p.setEstado(EstadoPostulante.RECHAZADO);
        p.setMotivoRechazo(motivo.trim());
        correo.encolar(p.getEmail(), "Su solicitud fue rechazada",
                "Hola " + p.getNombre() + ",\n\nSu solicitud para ser autoridad de mesa fue rechazada.\n\nMotivo: "
                        + p.getMotivoRechazo() + "\n");
        convocatoria.enviarReporteFinalSiCorresponde(); // CU8
        return p;
    }

    private Postulante pendiente(Long id) {
        exigirCerrada();
        Postulante p = buscar(id);
        if (p.getEstado() != EstadoPostulante.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El postulante ya fue evaluado");
        }
        return p;
    }

    private Postulante buscar(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Postulante no encontrado"));
    }

    private void exigirCerrada() {
        if (convocatoria.estado().estado() != EstadoConvocatoria.CERRADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La convocatoria todavía no está cerrada");
        }
    }

    /** Acepta formatos como +54 9 341 555-1234, 0341 5551234 o 3415551234 (10 dígitos sin 0 ni 15). */
    static boolean telefonoArgentino(String telefono) {
        String d = telefono.replaceAll("[\\s\\-().]", "");
        if (!d.matches("\\+?\\d+")) return false;
        boolean internacional = d.startsWith("+");
        d = d.replace("+", "");
        if (internacional && !d.startsWith("54")) return false;
        if (d.startsWith("54")) {
            d = d.substring(2);
            if (d.startsWith("9")) d = d.substring(1);
        }
        if (d.startsWith("0")) d = d.substring(1);
        return d.length() == 10;
    }
}
