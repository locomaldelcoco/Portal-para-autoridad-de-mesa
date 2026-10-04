package ar.portal.autoridadmesa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.portal.autoridadmesa.convocatoria.ConvocatoriaService;
import ar.portal.autoridadmesa.correo.CorreoRepository;
import ar.portal.autoridadmesa.correo.CorreoSaliente;
import ar.portal.autoridadmesa.correo.CorreoService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:test", "portal.eleccion.fecha=2099-06-01"})
@AutoConfigureMockMvc
@Import(PortalApiTest.RelojDePrueba.class)
class PortalApiTest {

    /** Reloj que el test puede mover para simular el paso del tiempo. */
    static class Mutable extends Clock {
        volatile Instant ahora;
        void en(String isoLocal) { ahora = java.time.LocalDateTime.parse(isoLocal).toInstant(ZoneOffset.UTC); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId z) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @TestConfiguration
    static class RelojDePrueba {
        @Bean @Primary Mutable relojDePrueba() { return new Mutable(); }
    }

    @Autowired MockMvc mvc;
    @Autowired Mutable reloj;
    @Autowired ConvocatoriaService convocatoria;
    @Autowired CorreoService correo;
    @Autowired CorreoRepository correos;
    @MockBean JavaMailSender servidorCorreo;

    static final String CHARLA = """
        {"nombre":"Charla 1","tema":"Funciones de la autoridad","fecha":"2099-05-10","horario":"18:00",
         "sedeNombre":"Biblioteca","sedeDireccion":"San Martin 123, Rosario"}""";

    static String postulante(String dni, String telefono) {
        return """
            {"distritoElectoral":"Santa Fe","nombre":"Ana","apellido":"Perez","dni":"%s","fechaNacimiento":"1990-01-01",
             "domicilio":"Calle 1","telefono":"%s","email":"ana@mail.com","antecedentesMesa":false,
             "estadoCapacitacion":"Sin capacitar","afiliado":false}""".formatted(dni, telefono);
    }

    MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder b) {
        return b.with(httpBasic("admin", "cambiar-esto")).contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder publico(MockHttpServletRequestBuilder b) {
        return b.contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void publicarCharlaValidaPermisosFechaPasadaYPosteriorAlaEleccion() throws Exception {
        reloj.en("2099-05-01T10:00:00");
        mvc.perform(publico(post("/api/charlas")).content(CHARLA)).andExpect(status().isUnauthorized());
        mvc.perform(admin(post("/api/charlas")).content(CHARLA.replace("2099-05-10", "2099-04-30")))
           .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.fecha").exists());
        mvc.perform(admin(post("/api/charlas")).content(CHARLA.replace("2099-05-10", "2099-06-02")))
           .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.fecha").exists());
    }

    @Test
    void flujoCompletoConvocatoriaPostulacionEvaluacionYCorreos() throws Exception {
        reloj.en("2099-05-01T10:00:00");
        mvc.perform(admin(post("/api/charlas")).content(CHARLA)).andExpect(status().isCreated());

        // Antes de la primera charla: no se puede postular
        mvc.perform(get("/api/convocatoria")).andExpect(jsonPath("$.estado").value("PROXIMA"));
        mvc.perform(publico(post("/api/postulantes")).content(postulante("30111222", "3415551234")))
           .andExpect(status().isConflict());

        // Convocatoria abierta
        reloj.en("2099-05-10T09:00:00");
        mvc.perform(get("/api/convocatoria")).andExpect(jsonPath("$.estado").value("ABIERTA"));
        mvc.perform(publico(post("/api/postulantes")).content(postulante("30111222", "+54 9 341 555-1234")))
           .andExpect(status().isCreated());
        mvc.perform(publico(post("/api/postulantes")).content(postulante("30111222", "3415551234")))
           .andExpect(status().isConflict());
        mvc.perform(publico(post("/api/postulantes")).content(postulante("40111222", "12345")))
           .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.telefono").exists());
        mvc.perform(publico(post("/api/postulantes")).content(postulante("abc", "3415551234")))
           .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.dni").exists());
        // Los datos personales no se consultan antes del cierre ni sin ser administrador
        mvc.perform(get("/api/postulantes")).andExpect(status().isUnauthorized());
        mvc.perform(admin(get("/api/postulantes"))).andExpect(status().isConflict());

        // Cierre (llegó la fecha de la última charla): CU1
        reloj.en("2099-05-10T18:00:00");
        mvc.perform(get("/api/convocatoria")).andExpect(jsonPath("$.estado").value("CERRADA"));
        mvc.perform(publico(post("/api/postulantes")).content(postulante("50111222", "3415551234")))
           .andExpect(status().isConflict());
        convocatoria.cerrarSiCorresponde();
        convocatoria.cerrarSiCorresponde(); // idempotente: no duplica el reporte
        assertThat(correos.count()).isEqualTo(1);

        // CU2 + CU3: consulta y evaluación
        mvc.perform(admin(get("/api/postulantes"))).andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].dni").value("30111222"));
        Long id = correos.count() == 1 ? 1L : null;
        mvc.perform(admin(post("/api/postulantes/" + id + "/rechazar")).content("{\"motivo\":\" \"}"))
           .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.motivo").exists());
        mvc.perform(admin(post("/api/postulantes/" + id + "/aprobar")).content("{}"))
           .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACEPTADO"));
        mvc.perform(admin(post("/api/postulantes/" + id + "/rechazar")).content("{\"motivo\":\"x\"}"))
           .andExpect(status().isConflict());

        // Reporte de cierre + aprobación + reporte final (CU8, ya no quedan pendientes)
        assertThat(correos.count()).isEqualTo(3);
        correo.enviarPendientes();
        verify(servidorCorreo, times(3)).send(any(SimpleMailMessage.class));
        assertThat(correos.findAll()).allMatch(c -> c.getEstado() == CorreoSaliente.Estado.ENVIADO);
    }
}
