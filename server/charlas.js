import { HttpError, datoInvalido } from './errores.js';

const texto = (v) => typeof v === 'string' ? v.trim() : '';

export function crearCharlas({ db, reloj, fechaEleccion }) {
  const aDto = (c) => ({
    id: c.id, nombre: c.nombre, tema: c.tema, fecha: c.fecha, horario: c.horario,
    sedeNombre: c.sede_nombre, sedeDireccion: c.sede_direccion,
  });

  return {
    /** Charlas ordenadas por fecha, con la forma que usan convocatoria y frontend. */
    todas() {
      return db.prepare('SELECT * FROM charla ORDER BY fecha, horario').all();
    },

    /** RF-02: las charlas que todavía no se realizaron. */
    proximas() {
      const ahora = reloj();
      return this.todas().filter((c) => `${c.fecha}T${c.horario}:00` >= ahora).map(aDto);
    },

    /** RF-01 */
    publicar(datos = {}) {
      const campos = {};
      for (const k of ['nombre', 'tema', 'sedeNombre', 'sedeDireccion']) {
        if (!texto(datos[k])) campos[k] = 'Este dato es obligatorio';
      }
      const fecha = texto(datos.fecha);
      const horario = texto(datos.horario).slice(0, 5);
      if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha) || Number.isNaN(Date.parse(fecha))) campos.fecha = 'Fecha inválida';
      if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(horario)) campos.horario = 'Horario inválido';
      if (Object.keys(campos).length) throw new HttpError(400, 'Hay datos inválidos', campos);

      if (`${fecha}T${horario}:00` < reloj()) throw datoInvalido('fecha', 'La fecha y el horario de la charla ya pasaron');
      if (fechaEleccion && fecha >= fechaEleccion) {
        throw datoInvalido('fecha', `La charla debe ser previa a la elección (${fechaEleccion})`);
      }

      const r = db.prepare(
        'INSERT INTO charla (nombre, tema, fecha, horario, sede_nombre, sede_direccion) VALUES (?, ?, ?, ?, ?, ?)',
      ).run(texto(datos.nombre), texto(datos.tema), fecha, horario, texto(datos.sedeNombre), texto(datos.sedeDireccion));
      return aDto(db.prepare('SELECT * FROM charla WHERE id = ?').get(r.lastInsertRowid));
    },

    existe(id) {
      return !!db.prepare('SELECT 1 FROM charla WHERE id = ?').get(id);
    },
  };
}
