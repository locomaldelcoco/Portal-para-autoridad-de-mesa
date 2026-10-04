package ar.portal.autoridadmesa.correo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro de cada correo a enviar: permite reintentar si el servidor falla y deja constancia del error. */
@Entity
public class CorreoSaliente {

    public enum Estado { PENDIENTE, ENVIADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String destinatario;
    @Column(nullable = false) private String asunto;
    @Column(nullable = false, length = 20000) private String cuerpo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Estado estado = Estado.PENDIENTE;
    private int intentos;
    @Column(length = 1000) private String ultimoError;
    @Column(nullable = false) private LocalDateTime creado;

    protected CorreoSaliente() {}

    public CorreoSaliente(String destinatario, String asunto, String cuerpo, LocalDateTime creado) {
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.creado = creado;
    }

    void marcarEnviado() { this.estado = Estado.ENVIADO; this.intentos++; this.ultimoError = null; }

    void registrarFallo(String error) {
        this.intentos++;
        this.ultimoError = error == null ? "Error desconocido" : error.substring(0, Math.min(error.length(), 1000));
    }

    public Long getId() { return id; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getCuerpo() { return cuerpo; }
    public Estado getEstado() { return estado; }
    public int getIntentos() { return intentos; }
    public String getUltimoError() { return ultimoError; }
}
