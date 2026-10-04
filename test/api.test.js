import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import crypto from 'node:crypto';
import { crearApp } from '../server/app.js';
import { telefonoArgentino } from '../server/postulantes.js';
import { crearServicioDirecciones } from '../server/direcciones.js';
import { cargarEjemplo } from '../server/ejemplo.js';

const ADMIN = 'Basic ' + Buffer.from('admin:cambiar-esto').toString('base64');
const CHARLA = {
  nombre: 'Charla 1', tema: 'Funciones de la autoridad', fecha: '2099-05-10', horario: '18:00',
  sedeNombre: 'Biblioteca', sedeDireccion: 'San Martin 123, Rosario',
};
const postulante = (dni, telefono) => ({
  distritoElectoral: 'Santa Fe', nombre: 'Ana', apellido: 'Perez', dni, fechaNacimiento: '1990-01-01',
  domicilio: 'Calle 1', telefono, email: 'ana@mail.com', antecedentesMesa: false,
  estadoCapacitacion: 'Sin capacitar', afiliado: false,
});

let ahora, enviados, portal, servidor, base;
before(async () => {
  enviados = [];
  portal = crearApp({
    config: {
      dbRuta: ':memory:', adminUsuario: 'admin', adminPassword: 'cambiar-esto', adminEmail: 'admin@x.com',
      fechaEleccion: '2099-06-01', remitente: 'no-reply@x.com', claveCifrado: crypto.randomBytes(32).toString('base64'),
    },
    reloj: () => ahora,
    transporte: { sendMail: async (m) => { enviados.push(m); } },
    direcciones: {
      buscar: async (q) => {
        if (q === 'caido') throw new Error('sin red');
        return [{ direccion: `${q.toUpperCase()} 123`, localidad: 'CABA' }];
      },
      ubicar: async (q) => {
        if (q === 'caido') throw new Error('sin red');
        return q === 'nada' ? null : { direccion: q.toUpperCase(), localidad: 'CABA', lat: -34.6, lng: -58.38 };
      },
    },
  });
  servidor = portal.app.listen(0);
  base = `http://localhost:${servidor.address().port}`;
});
after(() => servidor.close());

async function llamar(metodo, ruta, { cuerpo, admin = false } = {}) {
  const r = await fetch(base + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(admin ? { Authorization: ADMIN } : {}) },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  return { status: r.status, datos: await r.json() };
}

test('teléfonos argentinos', () => {
  for (const ok of ['3415551234', '0341 555-1234', '+54 9 341 555-1234', '(011) 4555-1234']) assert.ok(telefonoArgentino(ok), ok);
  for (const mal of ['12345', '+1 555 123 4567', 'abc', '']) assert.ok(!telefonoArgentino(mal), mal);
});

test('publicar charla: permisos, fecha pasada y posterior a la elección', async () => {
  ahora = '2099-05-01T10:00:00';
  assert.equal((await llamar('POST', '/api/charlas', { cuerpo: CHARLA })).status, 401);
  let r = await llamar('POST', '/api/charlas', { cuerpo: { ...CHARLA, fecha: '2099-04-30' }, admin: true });
  assert.equal(r.status, 400); assert.ok(r.datos.campos.fecha);
  r = await llamar('POST', '/api/charlas', { cuerpo: { ...CHARLA, fecha: '2099-06-02' }, admin: true });
  assert.equal(r.status, 400); assert.ok(r.datos.campos.fecha);
  r = await llamar('POST', '/api/charlas', { cuerpo: { nombre: '' }, admin: true });
  assert.equal(r.status, 400); assert.ok(r.datos.campos.nombre);
});

test('flujo completo: convocatoria, postulación, evaluación y correos', async () => {
  ahora = '2099-05-01T10:00:00';
  assert.equal((await llamar('POST', '/api/charlas', { cuerpo: CHARLA, admin: true })).status, 201);
  assert.equal((await llamar('GET', '/api/charlas')).datos.length, 1);

  // Antes de la primera charla no se puede postular
  assert.equal((await llamar('GET', '/api/convocatoria')).datos.estado, 'PROXIMA');
  assert.equal((await llamar('POST', '/api/postulantes', { cuerpo: postulante('30111222', '3415551234') })).status, 409);

  // Abierta
  ahora = '2099-05-10T09:00:00';
  assert.equal((await llamar('GET', '/api/convocatoria')).datos.estado, 'ABIERTA');
  assert.equal((await llamar('POST', '/api/postulantes', { cuerpo: postulante('30111222', '+54 9 341 555-1234') })).status, 201);
  assert.equal((await llamar('POST', '/api/postulantes', { cuerpo: postulante('30111222', '3415551234') })).status, 409);
  let r = await llamar('POST', '/api/postulantes', { cuerpo: postulante('40111222', '12345') });
  assert.equal(r.status, 400); assert.ok(r.datos.campos.telefono);
  r = await llamar('POST', '/api/postulantes', { cuerpo: postulante('abc', '3415551234') });
  assert.equal(r.status, 400); assert.ok(r.datos.campos.dni);
  r = await llamar('POST', '/api/postulantes', { cuerpo: { ...postulante('50111222', '3415551234'), afiliado: true } });
  assert.equal(r.status, 400); assert.ok(r.datos.campos.partido);

  // Los datos personales no se consultan sin ser administrador ni antes del cierre
  assert.equal((await llamar('GET', '/api/postulantes')).status, 401);
  assert.equal((await llamar('GET', '/api/postulantes', { admin: true })).status, 409);

  // DNI y domicilio están cifrados en la base
  const fila = portal.db.prepare('SELECT dni, domicilio FROM postulante').get();
  assert.ok(!fila.dni.includes('30111222') && !fila.domicilio.includes('Calle'));

  // Cierre: CU1
  ahora = '2099-05-10T18:00:00';
  assert.equal((await llamar('GET', '/api/convocatoria')).datos.estado, 'CERRADA');
  assert.equal((await llamar('POST', '/api/postulantes', { cuerpo: postulante('60111222', '3415551234') })).status, 409);
  portal.convocatoria.cerrarSiCorresponde();
  portal.convocatoria.cerrarSiCorresponde(); // idempotente
  assert.equal(portal.db.prepare('SELECT COUNT(*) n FROM correo').get().n, 1);

  // CU2 + CU3
  r = await llamar('GET', '/api/postulantes', { admin: true });
  assert.equal(r.status, 200); assert.equal(r.datos.length, 1); assert.equal(r.datos[0].dni, '30111222');
  const id = r.datos[0].id;
  r = await llamar('POST', `/api/postulantes/${id}/rechazar`, { cuerpo: { motivo: ' ' }, admin: true });
  assert.equal(r.status, 400); assert.ok(r.datos.campos.motivo);
  r = await llamar('POST', `/api/postulantes/${id}/aprobar`, { cuerpo: {}, admin: true });
  assert.equal(r.status, 200); assert.equal(r.datos.estado, 'ACEPTADO');
  assert.equal((await llamar('POST', `/api/postulantes/${id}/rechazar`, { cuerpo: { motivo: 'x' }, admin: true })).status, 409);

  // Reporte de cierre + aprobación + reporte final (CU8)
  assert.equal(portal.db.prepare('SELECT COUNT(*) n FROM correo').get().n, 3);
  await portal.correo.enviarPendientes();
  assert.equal(enviados.length, 3);
  assert.equal(portal.db.prepare("SELECT COUNT(*) n FROM correo WHERE estado = 'ENVIADO'").get().n, 3);
});

test('si el servidor de correo falla, el correo queda pendiente y se reintenta', async () => {
  const caido = crearApp({
    config: { dbRuta: ':memory:', adminUsuario: 'a', adminPassword: 'b', adminEmail: 'e@x.com', remitente: 'r@x.com',
      claveCifrado: crypto.randomBytes(32).toString('base64') },
    reloj: () => ahora,
    transporte: { sendMail: async () => { throw new Error('SMTP caído'); } },
  });
  caido.correo.encolar('x@y.com', 'Prueba', 'cuerpo');
  await caido.correo.enviarPendientes();
  const c = caido.db.prepare('SELECT estado, intentos, ultimo_error FROM correo').get();
  assert.equal(c.estado, 'PENDIENTE'); assert.equal(c.intentos, 1); assert.match(c.ultimo_error, /SMTP caído/);
});

test('sugerencias de direcciones: solo admin, mínimo 3 letras y falla controlada', async () => {
  assert.equal((await llamar('GET', '/api/direcciones?q=corrientes')).status, 401);
  assert.deepEqual((await llamar('GET', '/api/direcciones?q=co', { admin: true })).datos, []);
  const r = await llamar('GET', '/api/direcciones?q=corrientes', { admin: true });
  assert.equal(r.status, 200); assert.equal(r.datos[0].direccion, 'CORRIENTES 123');
  const caido = await llamar('GET', '/api/direcciones?q=caido', { admin: true });
  assert.equal(caido.status, 502); assert.match(caido.datos.mensaje, /manualmente/);
});

test('ubicación de una sede: es pública y falla de forma controlada', async () => {
  const r = await llamar('GET', '/api/ubicacion?direccion=Corrientes 1530');
  assert.equal(r.status, 200); assert.equal(r.datos.lat, -34.6); assert.equal(r.datos.lng, -58.38);
  assert.equal((await llamar('GET', '/api/ubicacion?direccion=nada')).status, 404);
  assert.equal((await llamar('GET', '/api/ubicacion?direccion=caido')).status, 502);
  assert.equal((await llamar('GET', '/api/ubicacion?direccion=a')).status, 400);
});

test('la USIG: se envía la dirección y se interpretan las coordenadas de la respuesta', async () => {
  let pedida;
  const falsoFetch = async (url) => {
    pedida = url;
    return { ok: true, json: async () => ({ direccionesNormalizadas: [
      { direccion: 'CORRIENTES AV. 123', nombre_localidad: 'CABA', coordenadas: { x: '99000', y: '100000' } }, // fuera de Argentina
      { direccion: 'CORRIENTES AV. 1530', nombre_localidad: 'CABA', coordenadas: { x: '-58.3877', y: '-34.6037' } },
      { calle: 'sin campo direccion' },
    ] }) };
  };
  const usig = crearServicioDirecciones(falsoFetch);
  assert.equal((await usig.buscar('corrientes')).length, 2);
  assert.equal(pedida.hostname, 'servicios.usig.buenosaires.gob.ar');
  assert.equal(pedida.searchParams.get('geocodificar'), 'true');
  const u = await usig.ubicar('corrientes 1530');
  assert.deepEqual(u, { direccion: 'CORRIENTES AV. 1530', localidad: 'CABA', lat: -34.6037, lng: -58.3877 });
  assert.equal(pedida.searchParams.get('direccion'), 'corrientes 1530');
  await assert.rejects(crearServicioDirecciones(async () => ({ ok: false, status: 500 })).buscar('x'), /500/);
});

test('datos de ejemplo: escenario abierta y escenario cerrada', async () => {
  for (const [escenario, estadoEsperado] of [['abierta', 'ABIERTA'], ['cerrada', 'CERRADA']]) {
    ahora = '2099-05-10T10:00:00';
    const p = crearApp({
      config: { dbRuta: ':memory:', adminUsuario: 'a', adminPassword: 'b', adminEmail: 'e@x.com', remitente: 'r@x.com',
        claveCifrado: crypto.randomBytes(32).toString('base64') },
      reloj: () => ahora,
    });
    assert.equal(cargarEjemplo(p, escenario), true);
    assert.equal(cargarEjemplo(p, escenario), false); // no se duplican
    assert.equal(p.convocatoria.estado().estado, estadoEsperado);
    assert.equal(p.db.prepare('SELECT COUNT(*) n FROM charla').get().n, 3);
    assert.equal(p.repo.todos().length, 3);
  }
});
