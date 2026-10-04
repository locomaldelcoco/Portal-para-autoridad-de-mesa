import path from 'node:path';
import nodemailer from 'nodemailer';
import { crearApp } from './app.js';
import { ahoraArgentina, leerConfig } from './config.js';
import { cargarEjemplo } from './ejemplo.js';

const escenario = process.argv.find((a) => a.startsWith('--ejemplo='))?.split('=')[1] ?? 'abierta';
if (!['abierta', 'cerrada'].includes(escenario)) {
  console.error('Uso: node server/index.js [--ejemplo=abierta|cerrada]');
  process.exit(1);
}

const config = leerConfig();
// Cada escenario usa su propia base para no mezclar datos
if (escenario === 'cerrada' && !process.env.PORTAL_DB) {
  config.dbRuta = path.join(path.dirname(config.dbRuta), 'portal-cerrada.db');
}

const transporte = config.smtp ? nodemailer.createTransport(config.smtp) : null;
const portal = crearApp({ config, reloj: ahoraArgentina, transporte });
if (cargarEjemplo(portal, escenario)) console.log(`Datos de ejemplo cargados (convocatoria ${escenario}).`);
portal.iniciarTareas();

portal.app.listen(config.puerto, () => {
  console.log(`Portal en http://localhost:${config.puerto}  (administración: /admin.html)`);
  if (!transporte) console.warn('SMTP_HOST no configurado: los correos quedarán pendientes hasta configurarlo.');
  if (config.adminPassword === 'cambiar-esto') console.warn('Usando la contraseña de administrador por defecto: definir PORTAL_ADMIN_PASSWORD.');
});
