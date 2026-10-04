import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

/** Lee la configuración de variables de entorno (ver README). */
export function leerConfig(env = process.env) {
  const dirDatos = env.PORTAL_DATOS_DIR ?? 'data';
  return {
    puerto: Number(env.PORT ?? 3000),
    dbRuta: env.PORTAL_DB ?? path.join(dirDatos, 'portal.db'),
    adminUsuario: env.PORTAL_ADMIN_USER ?? 'admin',
    adminPassword: env.PORTAL_ADMIN_PASSWORD ?? 'cambiar-esto',
    adminEmail: env.PORTAL_ADMIN_EMAIL ?? 'administrador@organismo.invalid',
    fechaEleccion: env.PORTAL_ELECCION_FECHA || null, // AAAA-MM-DD
    remitente: env.PORTAL_CORREO_REMITENTE ?? 'no-reply@organismo.invalid',
    smtp: env.SMTP_HOST
      ? {
          host: env.SMTP_HOST,
          port: Number(env.SMTP_PORT ?? 587),
          auth: env.SMTP_USER ? { user: env.SMTP_USER, pass: env.SMTP_PASSWORD } : undefined,
        }
      : null,
    claveCifrado: env.PORTAL_CIFRADO_CLAVE ?? claveLocal(dirDatos),
  };
}

/** Sin PORTAL_CIFRADO_CLAVE se genera una clave y se guarda en data/ (solo para desarrollo). */
function claveLocal(dirDatos) {
  const archivo = path.join(dirDatos, 'clave-cifrado.key');
  if (fs.existsSync(archivo)) return fs.readFileSync(archivo, 'utf8').trim();
  fs.mkdirSync(dirDatos, { recursive: true });
  const clave = crypto.randomBytes(32).toString('base64');
  fs.writeFileSync(archivo, clave, { mode: 0o600 });
  return clave;
}

/** "Ahora" en hora argentina, como texto AAAA-MM-DDTHH:MM:SS (se compara como texto). */
export function ahoraArgentina() {
  const p = Object.fromEntries(
    new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Argentina/Buenos_Aires', hourCycle: 'h23',
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit',
    }).formatToParts(new Date()).map((x) => [x.type, x.value]),
  );
  return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}:${p.second}`;
}
