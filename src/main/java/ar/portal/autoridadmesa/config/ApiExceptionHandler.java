package ar.portal.autoridadmesa.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Todos los errores salen como JSON: {"mensaje": "...", "campos": {"dni": "..."}} */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Hay datos inválidos", "campos", campos));
    }

    @ExceptionHandler(ValidacionException.class)
    ResponseEntity<Map<String, Object>> dato(ValidacionException ex) {
        return ResponseEntity.badRequest().body(
                Map.of("mensaje", ex.getMessage(), "campos", Map.of(ex.campo(), ex.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> jsonMalo(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El formato de los datos es incorrecto"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> estado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("mensaje", String.valueOf(ex.getReason())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex) {
        // Respaldo ante dos inscripciones simultáneas con el mismo DNI
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "Ya existe una inscripción con ese DNI"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        if (ex instanceof ErrorResponse er) { // 404, 405, etc. de Spring
            return ResponseEntity.status(er.getStatusCode()).body(Map.of("mensaje", er.getBody().getTitle() == null ? "Error" : er.getBody().getTitle()));
        }
        log.error("Error inesperado", ex);
        return ResponseEntity.internalServerError().body(Map.of("mensaje", "Algo salió mal, intente nuevamente"));
    }
}
