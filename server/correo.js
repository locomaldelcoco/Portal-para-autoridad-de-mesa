/**
 * RF-15. Todo correo saliente pasa por el servidor de correo institucional.
 * Los correos se encolan en la tabla `correo` y un proceso programado los envía:
 * si el servidor falla se registra el error y se reintenta en la siguiente pasada.
 */
export function crearCorreo({ db, reloj, remitente, transporte }) {
  let enviando = false;

  return {
    encolar(destinatario, asunto, cuerpo) {
      db.prepare('INSERT INTO correo (destinatario, asunto, cuerpo, creado) VALUES (?, ?, ?, ?)')
        .run(destinatario, asunto, cuerpo, reloj());
    },

    async enviarPendientes() {
      if (enviando) return;
      enviando = true;
      try {
        const pendientes = db.prepare("SELECT * FROM correo WHERE estado = 'PENDIENTE' ORDER BY id").all();
        for (const c of pendientes) {
          try {
            if (!transporte) throw new Error('Servidor de correo no configurado');
            await transporte.sendMail({ from: remitente, to: c.destinatario, subject: c.asunto, text: c.cuerpo });
            db.prepare("UPDATE correo SET estado = 'ENVIADO', intentos = intentos + 1, ultimo_error = NULL WHERE id = ?").run(c.id);
          } catch (e) {
            db.prepare('UPDATE correo SET intentos = intentos + 1, ultimo_error = ? WHERE id = ?')
              .run(String(e.message ?? e).slice(0, 1000), c.id);
            console.warn(`Falló el envío del correo ${c.id}: ${e.message}`);
          }
        }
      } finally {
        enviando = false;
      }
    },
  };
}
