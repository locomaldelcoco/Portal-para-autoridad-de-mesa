package ar.portal.autoridadmesa.convocatoria;

import ar.portal.autoridadmesa.charla.Charla;
import ar.portal.autoridadmesa.charla.CharlaRepository;
import ar.portal.autoridadmesa.correo.CorreoService;
import ar.portal.autoridadmesa.postulante.EstadoPostulante;
import ar.portal.autoridadmesa.postulante.Postulante;
import ar.portal.autoridadmesa.postulante.PostulanteRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-03 / RF-04: la convocatoria se abre el día de la primera charla y se cierra
 * en la fecha y hora de la última charla cargada.
 * CU1 (cierre + reporte) y CU8 (reporte final) también viven acá.
 */
@Service
public class ConvocatoriaService {

    private final CharlaRepository charlas;
    private final ConvocatoriaRepository convocatoria;
    private final PostulanteRepository postulantes;
    private final CorreoService correo;
    private final Clock reloj;
    private final String emailAdmin;

    public ConvocatoriaService(CharlaRepository charlas, ConvocatoriaRepository convocatoria,
                               PostulanteRepository postulantes, CorreoService correo, Clock reloj,
                               @Value("${portal.admin.email}") String emailAdmin) {
        this.charlas = charlas;
        this.convocatoria = convocatoria;
        this.postulantes = postulantes;
        this.correo = correo;
        this.reloj = reloj;
        this.emailAdmin = emailAdmin;
    }

    @Transactional(readOnly = true)
    public ConvocatoriaDto estado() {
        List<Charla> todas = charlas.findAllByOrderByFechaAscHorarioAsc();
        if (todas.isEmpty()) return new ConvocatoriaDto(EstadoConvocatoria.SIN_CHARLAS, null, null);

        LocalDateTime apertura = todas.get(0).getFecha().atStartOfDay();
        LocalDateTime cierre = todas.get(todas.size() - 1).fechaHora();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        boolean cerrada = ahora.isAfter(cierre) || ahora.isEqual(cierre) || cierreYaProcesado();

        EstadoConvocatoria estado = cerrada ? EstadoConvocatoria.CERRADA
                : ahora.isBefore(apertura) ? EstadoConvocatoria.PROXIMA : EstadoConvocatoria.ABIERTA;
        return new ConvocatoriaDto(estado, apertura, cierre);
    }

    /** CU1, ejecutado por el scheduler: cierra, genera el reporte de postulantes y lo envía al administrador. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 5_000)
    @Transactional
    public void cerrarSiCorresponde() {
        if (estado().estado() != EstadoConvocatoria.CERRADA) return;
        Convocatoria c = fila();
        if (c.isCierreProcesado()) return;

        List<Postulante> inscriptos = postulantes.findAllByOrderByIdAsc();
        correo.encolar(emailAdmin, "Cierre de convocatoria: reporte de postulantes",
                armarReporte("Postulantes inscriptos", inscriptos));
        c.setCierreProcesado(true);
        convocatoria.save(c);
    }

    /** CU8: cuando no queda ningún postulante pendiente, se envía al administrador la lista de aprobados. */
    @Transactional
    public void enviarReporteFinalSiCorresponde() {
        Convocatoria c = fila();
        if (c.isReporteFinalEncolado() || postulantes.existsByEstado(EstadoPostulante.PENDIENTE)) return;

        correo.encolar(emailAdmin, "Reporte final: postulantes confirmados",
                armarReporte("Postulantes aprobados", postulantes.findByEstadoOrderByIdAsc(EstadoPostulante.ACEPTADO)));
        c.setReporteFinalEncolado(true);
        convocatoria.save(c);
    }

    private static String armarReporte(String titulo, List<Postulante> lista) {
        StringBuilder sb = new StringBuilder(titulo).append(" (").append(lista.size()).append(")\n\n");
        if (lista.isEmpty()) {
            return sb.append("N/A\n").toString();
        }
        for (Postulante p : lista) {
            sb.append(p.getApellido()).append(", ").append(p.getNombre())
              .append(" | DNI ").append(p.getDni())
              .append(" | ").append(p.getDistritoElectoral())
              .append(" | ").append(p.getEmail())
              .append(" | ").append(p.getTelefono()).append('\n');
        }
        return sb.toString();
    }

    private boolean cierreYaProcesado() {
        return convocatoria.findById(Convocatoria.ID).map(Convocatoria::isCierreProcesado).orElse(false);
    }

    private Convocatoria fila() {
        return convocatoria.findById(Convocatoria.ID).orElseGet(() -> convocatoria.save(new Convocatoria()));
    }
}
