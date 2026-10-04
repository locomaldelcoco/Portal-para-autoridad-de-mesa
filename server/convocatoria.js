import { transaccion } from './db.js';

/**
 * RF-03 / RF-04: la convocatoria se abre el día de la primera charla y se cierra
 * en la fecha y hora de la última charla cargada.
 * También contiene CU1 (cierre + reporte) y CU8 (reporte final).
 */
export function crearConvocatoria({ db, reloj, charlas, postulantes, correo, emailAdmin }) {
  const fila = () => db.prepare('SELECT * FROM convocatoria WHERE id = 1').get();

  function estado() {
    const lista = charlas.todas();
    if (!lista.length) return { estado: 'SIN_CHARLAS', apertura: null, cierre: null };

    const apertura = `${lista[0].fecha}T00:00:00`;
    const ultima = lista[lista.length - 1];
    const cierre = `${ultima.fecha}T${ultima.horario}:00`;
    const ahora = reloj();
    const cerrada = ahora >= cierre || fila().cierre_procesado === 1;
    return { estado: cerrada ? 'CERRADA' : ahora < apertura ? 'PROXIMA' : 'ABIERTA', apertura, cierre };
  }

  function armarReporte(titulo, lista) {
    const encabezado = `${titulo} (${lista.length})\n\n`;
    if (!lista.length) return encabezado + 'N/A\n';
    return encabezado + lista.map((p) =>
      `${p.apellido}, ${p.nombre} | DNI ${p.dni} | ${p.distritoElectoral} | ${p.email} | ${p.telefono}\n`).join('');
  }

  return {
    estado,

    /** CU1, lo ejecuta el scheduler: cierra, genera el reporte de postulantes y se lo envía al administrador. */
    cerrarSiCorresponde() {
      transaccion(db, () => {
        if (estado().estado !== 'CERRADA' || fila().cierre_procesado === 1) return;
        correo.encolar(emailAdmin, 'Cierre de convocatoria: reporte de postulantes',
          armarReporte('Postulantes inscriptos', postulantes.todos()));
        db.exec('UPDATE convocatoria SET cierre_procesado = 1 WHERE id = 1');
      });
    },

    /** CU8: cuando no queda ningún postulante pendiente, se envía al administrador la lista de aprobados. */
    enviarReporteFinalSiCorresponde() {
      if (fila().reporte_final_encolado === 1 || postulantes.hayPendientes()) return;
      correo.encolar(emailAdmin, 'Reporte final: postulantes confirmados',
        armarReporte('Postulantes aprobados', postulantes.aceptados()));
      db.exec('UPDATE convocatoria SET reporte_final_encolado = 1 WHERE id = 1');
    },
  };
}
