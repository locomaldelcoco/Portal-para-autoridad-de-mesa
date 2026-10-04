package ar.portal.autoridadmesa;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:test")
@AutoConfigureMockMvc
class PortalApiTest {

    @Autowired MockMvc mvc;

    static final String CHARLA = """
        {"titulo":"Charla 1","fechaHora":"2099-05-10T18:00","lugar":"Biblioteca","direccion":"San Martin 123",
         "localidad":"Rosario","temas":["Funciones","Escrutinio"]}""";

    static String inscripcion(String dni, LocalDate nacimiento) {
        return """
            {"nombre":"Ana","apellido":"Perez","dni":"%s","fechaNacimiento":"%s","email":"ana@mail.com",
             "telefono":"341555","domicilio":"Calle 1","localidad":"Rosario","aceptaTerminos":true}"""
            .formatted(dni, nacimiento);
    }

    @Test
    void crearCharlaRequiereAdminYQuedaVisible() throws Exception {
        mvc.perform(post("/api/charlas").contentType(MediaType.APPLICATION_JSON).content(CHARLA))
           .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/charlas").with(httpBasic("admin", "cambiar-esto"))
                .contentType(MediaType.APPLICATION_JSON).content(CHARLA))
           .andExpect(status().isCreated());
        mvc.perform(get("/api/charlas")).andExpect(status().isOk())
           .andExpect(jsonPath("$[0].temas.length()").value(2));
    }

    @Test
    void inscripcionValidaDuplicadaYMenorDeEdad() throws Exception {
        String ok = inscripcion("30111222", LocalDate.of(1990, 1, 1));
        mvc.perform(post("/api/inscripciones").contentType(MediaType.APPLICATION_JSON).content(ok))
           .andExpect(status().isCreated());
        mvc.perform(post("/api/inscripciones").contentType(MediaType.APPLICATION_JSON).content(ok))
           .andExpect(status().isConflict());
        mvc.perform(post("/api/inscripciones").contentType(MediaType.APPLICATION_JSON)
                .content(inscripcion("40111222", LocalDate.now().minusYears(17))))
           .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/inscripciones").contentType(MediaType.APPLICATION_JSON)
                .content(inscripcion("abc", LocalDate.of(1990, 1, 1))))
           .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.dni").exists());
    }

    @Test
    void listarInscriptosSoloAdmin() throws Exception {
        mvc.perform(get("/api/inscripciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/inscripciones").with(httpBasic("admin", "cambiar-esto"))).andExpect(status().isOk());
    }
}
