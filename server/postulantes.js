import { DISTRITOS } from './distritos.js';
import { HttpError, datoInvalido } from './errores.js';
import { transaccion } from './db.js';

const esTexto = (v, max) => typeof v === 'string' && v.trim() !== '' && v.trim().length <= max;

/** Acepta +54 9 341 555-1234, 0341 5551234 o 3415551234 (10 dígitos sin 0 ni 15). */
export function telefonoArgentino(telefono) {
  let d = telefono.replace(/[\s\-().]/g, '');
  if (!/^\+?\d+$/.test(d)) return false;
  const internacional = d.startsWith('+');
  d = d.replace('+', '');
  if (internacional && !d.startsWith('54')) return false;
  if (d.startsWith('54')) {
    d = d.slice(2);
    if (d.startsWith('9')) d = d.slice(1);
  }
  if (d.startsWith('0')) d = d.slice(1);
  return d.length === 10;
}

/** Acceso a datos de postulantes. DNI, domicilio y partido se cifran al guardar y se descifran al leer (RNF-04). */
export function crearRepositorioPostulantes({ db, cifrado }) {
  const aObjeto = (p) => ({
    id: p.id,
    distritoElectoral: p.distrito_electoral,
    nombre: p.nombre,
    apellido: p.apellido,
    dni: cifrado.descifrar(p.dni),
    fechaNacimiento: p.fecha_nacimiento,
    domicilio: cifrado.descifrar(p.domicilio),
    telefono: p.telefono,
    email: p.email,
    antecedentesMesa: p.antecedentes_mesa === 1,
    estadoCapacitacion: p.estado_capacitacion,
    afiliado: p.afiliado === 1,
    partido: p.partido ? cifrado.descifrar(p.partido) : null,
    charlaInteresId: p.charla_interes_id,
    estado: p.estado,
    motivoRechazo: p.motivo_rechazo,
    fechaRegistro: p.fecha_registro,
  });

  return {
    todos: () => db.prepare('SELECT * FROM postulante ORDER BY id').all().map(aObjeto),
    aceptados: () => db.prepare("SELECT * FROM postulante WHERE estado = 'ACEPTADO' ORDER BY id").all().map(aObjeto),
    hayPendientes: () => !!db.prepare("SELECT 1 FROM postulante WHERE estado = 'PENDIENTE' LIMIT 1").get(),
    buscar(id) {
      const p = db.prepare('SELECT * FROM postulante WHERE id = ?').get(id);
      return p ? aObjeto(p) : null;
    },
    existeDni: (dni) => !!db.prepare('SELECT 1 FROM postulante WHERE dni_hash = ?').get(cifrado.hash(dni)),
    insertar(p) {
      return Number(db.prepare(`INSERT INTO postulante (distrito_electoral, nombre, apellido, dni, dni_hash,
          fecha_nacimiento, domicilio, telefono, email, antecedentes_mesa, estado_capacitacion, afiliado, partido,
          charla_interes_id, fecha_registro) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`).run(
        p.distritoElectoral, p.nombre, p.apellido, cifrado.cifrar(p.dni), cifrado.hash(p.dni), p.fechaNacimiento,
        cifrado.cifrar(p.domicilio), p.telefono, p.email, p.antecedentesMesa ? 1 : 0, p.estadoCapacitacion,
        p.afiliado ? 1 : 0, p.partido ? cifrado.cifrar(p.partido) : null, p.charlaInteresId ?? null, p.fechaRegistro,
      ).lastInsertRowid);
    },
    cambiarEstado: (id, estado, motivo = null) =>
      db.prepare('UPDATE postulante SET estado = ?, motivo_rechazo = ? WHERE id = ?').run(estado, motivo, id),
  };
}

export function crearPostulantes({ db, repo, charlas, convocatoria, correo, reloj }) {
  const exigirCerrada = () => {
    if (convocatoria.estado().estado !== 'CERRADA') {
      throw new HttpError(409, 'La convocatoria todavía no está cerrada');
    }
  };

  const pendiente = (id) => {
    exigirCerrada();
    const p = repo.buscar(id);
    if (!p) throw new HttpError(404, 'Postulante no encontrado');
    if (p.estado !== 'PENDIENTE') throw new HttpError(409, 'El postulante ya fue evaluado');
    return p;
  };

  /** Validación de formato del formulario (RF-05). Devuelve {campo: mensaje}. */
  function validar(d) {
    const e = {};
    const obligatorio = (k, max = 200) => { if (!esTexto(d[k], max)) e[k] = 'Dato obligatorio (máx. ' + max + ' caracteres)'; };
    ['distritoElectoral', 'estadoCapacitacion'].forEach((k) => obligatorio(k, 100));
    ['nombre', 'apellido'].forEach((k) => obligatorio(k, 100));
    obligatorio('domicilio', 200);
    obligatorio('telefono', 30);
    if (typeof d.dni !== 'string' || !/^\d{7,8}$/.test(d.dni)) e.dni = 'El DNI debe tener 7 u 8 dígitos, sin puntos';
    if (typeof d.email !== 'string' || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.email.trim())) {
      e.email = 'El correo debe tener formato usuario@dominio';
    }
    if (typeof d.fechaNacimiento !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(d.fechaNacimiento)
        || Number.isNaN(Date.parse(d.fechaNacimiento))) {
      e.fechaNacimiento = 'Fecha inválida';
    }
    for (const k of ['antecedentesMesa', 'afiliado']) {
      if (typeof d[k] !== 'boolean') e[k] = 'Dato obligatorio';
    }
    if (d.partido != null && (typeof d.partido !== 'string' || d.partido.length > 100)) e.partido = 'Partido inválido';
    return e;
  }

  return {
    /** RF-05: solo con la convocatoria abierta (RF-03 / RF-04). */
    registrar(d = {}) {
      const campos = validar(d);
      if (Object.keys(campos).length) throw new HttpError(400, 'Hay datos inválidos', campos);
      if (!campos.fechaNacimiento && d.fechaNacimiento >= reloj().slice(0, 10)) {
        throw datoInvalido('fechaNacimiento', 'La fecha de nacimiento debe ser pasada');
      }
      if (convocatoria.estado().estado !== 'ABIERTA') {
        throw new HttpError(409, 'La inscripción no está habilitada en este momento');
      }
      if (!DISTRITOS.includes(d.distritoElectoral)) throw datoInvalido('distritoElectoral', 'El distrito electoral no es reconocido');
      if (!telefonoArgentino(d.telefono)) throw datoInvalido('telefono', 'El teléfono debe ser un número de la República Argentina');
      if (d.afiliado && !d.partido?.trim()) throw datoInvalido('partido', 'Indique el partido al que está afiliado');
      if (d.charlaInteresId != null && !charlas.existe(d.charlaInteresId)) {
        throw datoInvalido('charlaInteresId', 'La charla seleccionada no existe');
      }
      if (repo.existeDni(d.dni)) throw new HttpError(409, 'Ya existe una inscripción con ese DNI');

      const id = repo.insertar({
        ...d,
        nombre: d.nombre.trim(), apellido: d.apellido.trim(), domicilio: d.domicilio.trim(),
        telefono: d.telefono.trim(), email: d.email.trim(), estadoCapacitacion: d.estadoCapacitacion.trim(),
        partido: d.afiliado ? d.partido.trim() : null, fechaRegistro: reloj(),
      });
      return { id, estado: 'PENDIENTE' };
    },

    /** RF-08 / CU2: solo con la convocatoria cerrada. */
    consultar() { exigirCerrada(); return repo.todos(); },

    obtener(id) {
      exigirCerrada();
      const p = repo.buscar(id);
      if (!p) throw new HttpError(404, 'Postulante no encontrado');
      return p;
    },

    /** RF-09 / CU4 + CU6 */
    aprobar(id) {
      return transaccion(db, () => {
        const p = pendiente(id);
        repo.cambiarEstado(id, 'ACEPTADO');
        correo.encolar(p.email, 'Su solicitud fue aprobada',
          `Hola ${p.nombre},\n\nSu solicitud para ser autoridad de mesa fue aprobada.\n`);
        convocatoria.enviarReporteFinalSiCorresponde(); // CU8
        return repo.buscar(id);
      });
    },

    /** RF-11 / CU5 + CU7. El motivo es obligatorio. */
    rechazar(id, motivo) {
      if (typeof motivo !== 'string' || !motivo.trim()) throw datoInvalido('motivo', 'El motivo del rechazo es obligatorio');
      return transaccion(db, () => {
        const p = pendiente(id);
        repo.cambiarEstado(id, 'RECHAZADO', motivo.trim());
        correo.encolar(p.email, 'Su solicitud fue rechazada',
          `Hola ${p.nombre},\n\nSu solicitud para ser autoridad de mesa fue rechazada.\n\nMotivo: ${motivo.trim()}\n`);
        convocatoria.enviarReporteFinalSiCorresponde(); // CU8
        return repo.buscar(id);
      });
    },
  };
}
