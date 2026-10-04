package ar.portal.autoridadmesa.correo;

import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-15: todo correo saliente pasa por el servidor de correo institucional.
 * Los correos se encolan y un proceso programado los envía; si el servidor falla,
 * se registra el error y se reintenta en la siguiente pasada.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final CorreoRepository repo;
    private final ObjectProvider<JavaMailSender> servidor;
    private final Clock reloj;
    private final String remitente;

    public CorreoService(CorreoRepository repo, ObjectProvider<JavaMailSender> servidor, Clock reloj,
                         @Value("${portal.correo.remitente}") String remitente) {
        this.repo = repo;
        this.servidor = servidor;
        this.reloj = reloj;
        this.remitente = remitente;
    }

    /** Se une a la transacción de quien lo llama: si esa se revierte, el correo tampoco queda encolado. */
    @Transactional
    public void encolar(String destinatario, String asunto, String cuerpo) {
        repo.save(new CorreoSaliente(destinatario, asunto, cuerpo, LocalDateTime.now(reloj)));
    }

    @Scheduled(fixedDelayString = "${portal.correo.reintento-ms}", initialDelay = 10_000)
    @Transactional
    public void enviarPendientes() {
        for (CorreoSaliente c : repo.findByEstadoOrderByIdAsc(CorreoSaliente.Estado.PENDIENTE)) {
            try {
                JavaMailSender sender = servidor.getIfAvailable();
                if (sender == null) throw new IllegalStateException("Servidor de correo no configurado");
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(remitente);
                m.setTo(c.getDestinatario());
                m.setSubject(c.getAsunto());
                m.setText(c.getCuerpo());
                sender.send(m);
                c.marcarEnviado();
            } catch (Exception e) {
                c.registrarFallo(e.getMessage());
                log.warn("Fallo el envío del correo {} (intento {}): {}", c.getId(), c.getIntentos(), e.getMessage());
            }
        }
    }
}
