import crypto from 'node:crypto';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import { crearCharlas } from './charlas.js';
import { crearCifrado } from './cifrado.js';
import { crearConvocatoria } from './convocatoria.js';
import { crearCorreo } from './correo.js';
import { abrirDb } from './db.js';
import { DISTRITOS } from './distritos.js';
import { HttpError } from './errores.js';
import { crearPostulantes, crearRepositorioPostulantes } from './postulantes.js';

const publico = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'public');

const igual = (a, b) => {
  const ha = crypto.createHash('sha256').update(a).digest();
  const hb = crypto.createHash('sha256').update(b).digest();
  return crypto.timingSafeEqual(ha, hb);
};

/**
 * Arma toda la aplicación. `reloj` devuelve la hora argentina como AAAA-MM-DDTHH:MM:SS y
 * `transporte` es el servidor de correo (nodemailer); ambos se inyectan para poder probar.
 */
export function crearApp({ config, reloj, transporte }) {
  const db = abrirDb(config.dbRuta);
  const cifrado = crearCifrado(config.claveCifrado);
  const correo = crearCorreo({ db, reloj, remitente: config.remitente, transporte });
  const charlas = crearCharlas({ db, reloj, fechaEleccion: config.fechaEleccion });
  const repo = crearRepositorioPostulantes({ db, cifrado });
  const convocatoria = crearConvocatoria({ db, reloj, charlas, postulantes: repo, correo, emailAdmin: config.adminEmail });
  const postulantes = crearPostulantes({ db, repo, charlas, convocatoria, correo, reloj });

  /** RNF-03: las rutas del administrador piden usuario y contraseña (HTTP Basic). */
  const soloAdmin = (req, _res, next) => {
    const [tipo, valor] = (req.headers.authorization ?? '').split(' ');
    const [usuario, ...resto] = tipo === 'Basic' && valor ? Buffer.from(valor, 'base64').toString('utf8').split(':') : [];
    if (usuario !== undefined && igual(usuario, config.adminUsuario) && igual(resto.join(':'), config.adminPassword)) {
      return next();
    }
    next(new HttpError(401, 'Credenciales requeridas'));
  };

  const app = express();
  app.use(express.json({ limit: '100kb' }));

  // RNF-02: público
  app.get('/api/charlas', (_req, res) => res.json(charlas.proximas()));
  app.get('/api/convocatoria', (_req, res) => res.json(convocatoria.estado()));
  app.get('/api/distritos', (_req, res) => res.json(DISTRITOS));
  app.post('/api/postulantes', (req, res) => res.status(201).json(postulantes.registrar(req.body)));

  // RNF-03: administrador
  app.post('/api/charlas', soloAdmin, (req, res) => res.status(201).json(charlas.publicar(req.body)));
  app.get('/api/postulantes', soloAdmin, (_req, res) => res.json(postulantes.consultar()));
  app.get('/api/postulantes/:id', soloAdmin, (req, res) => res.json(postulantes.obtener(Number(req.params.id))));
  app.post('/api/postulantes/:id/aprobar', soloAdmin, (req, res) => res.json(postulantes.aprobar(Number(req.params.id))));
  app.post('/api/postulantes/:id/rechazar', soloAdmin, (req, res) =>
    res.json(postulantes.rechazar(Number(req.params.id), req.body?.motivo)));

  app.use('/api', (_req, _res, next) => next(new HttpError(404, 'No encontrado')));
  app.use(express.static(publico));

  // Todos los errores salen como JSON: {mensaje, campos?}
  app.use((err, _req, res, _next) => {
    if (err instanceof HttpError) return res.status(err.status).json({ mensaje: err.message, campos: err.campos });
    if (err.type === 'entity.parse.failed') return res.status(400).json({ mensaje: 'El formato de los datos es incorrecto' });
    if (err.code === 'ERR_SQLITE_ERROR' && /UNIQUE/.test(err.message)) {
      return res.status(409).json({ mensaje: 'Ya existe una inscripción con ese DNI' }); // dos inscripciones simultáneas
    }
    console.error('Error inesperado', err);
    res.status(500).json({ mensaje: 'Algo salió mal, intente nuevamente' });
  });

  return {
    app, correo, convocatoria, db,
    /** Tareas periódicas: cierre de convocatoria (CU1) y envío/reintento de correos. */
    iniciarTareas(cadaMs = 60_000) {
      const cerrar = () => { try { convocatoria.cerrarSiCorresponde(); } catch (e) { console.error(e); } };
      const timers = [setTimeout(cerrar, 5_000), setInterval(cerrar, cadaMs), setInterval(() => correo.enviarPendientes(), cadaMs)];
      return () => timers.forEach((t) => { clearTimeout(t); clearInterval(t); });
    },
  };
}
