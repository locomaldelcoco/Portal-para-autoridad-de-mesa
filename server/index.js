import nodemailer from 'nodemailer';
import { crearApp } from './app.js';
import { ahoraArgentina, leerConfig } from './config.js';

const config = leerConfig();
const transporte = config.smtp ? nodemailer.createTransport(config.smtp) : null;
const portal = crearApp({ config, reloj: ahoraArgentina, transporte });
portal.iniciarTareas();

portal.app.listen(config.puerto, () => {
  console.log(`Portal en http://localhost:${config.puerto}  (administración: /admin.html)`);
  if (!transporte) console.warn('SMTP_HOST no configurado: los correos quedarán pendientes hasta configurarlo.');
  if (config.adminPassword === 'cambiar-esto') console.warn('Usando la contraseña de administrador por defecto: definir PORTAL_ADMIN_PASSWORD.');
});
