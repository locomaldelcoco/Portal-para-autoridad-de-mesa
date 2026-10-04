/** Datos de ejemplo para el prototipo. Solo se cargan si todavía no hay charlas. */

const SEDES = [
  { sedeNombre: 'Teatro San Martín', sedeDireccion: 'Av. Corrientes 1530' },
  { sedeNombre: 'Legislatura Porteña', sedeDireccion: 'Perú 130' },
  { sedeNombre: 'Centro Cultural Recoleta', sedeDireccion: 'Junín 1930' },
];

const TEMAS = [
  ['Qué es una autoridad de mesa', 'Funciones y responsabilidades'],
  ['Apertura y cierre de la mesa', 'Procedimiento el día de la elección'],
  ['Escrutinio y documentación', 'Conteo de votos y actas'],
];

const POSTULANTES = [
  { nombre: 'Lucía', apellido: 'Fernández', dni: '30111222', distritoElectoral: 'Ciudad Autónoma de Buenos Aires', antecedentesMesa: true, estadoCapacitacion: 'Capacitada', afiliado: false },
  { nombre: 'Martín', apellido: 'Gómez', dni: '28555666', distritoElectoral: 'Buenos Aires', antecedentesMesa: false, estadoCapacitacion: 'Sin capacitar', afiliado: true, partido: 'Partido de ejemplo' },
  { nombre: 'Carla', apellido: 'Rossi', dni: '35222333', distritoElectoral: 'Santa Fe', antecedentesMesa: false, estadoCapacitacion: 'En curso', afiliado: false },
];

const sumarDias = (fecha, dias) => {
  const [a, m, d] = fecha.split('-').map(Number);
  return new Date(Date.UTC(a, m - 1, d + dias)).toISOString().slice(0, 10);
};

/**
 * escenario 'abierta': la convocatoria está abierta (primera charla hoy): sirve para ver charlas, mapa e inscripción.
 * escenario 'cerrada': las charlas ya pasaron: sirve para consultar y evaluar postulantes.
 */
export function cargarEjemplo({ db, repo, reloj }, escenario) {
  if (db.prepare('SELECT COUNT(*) AS n FROM charla').get().n > 0) return false;

  const hoy = reloj().slice(0, 10);
  const dias = escenario === 'cerrada' ? [-9, -5, -2] : [0, 7, 14];
  const insertar = db.prepare(
    'INSERT INTO charla (nombre, tema, fecha, horario, sede_nombre, sede_direccion) VALUES (?, ?, ?, ?, ?, ?)');
  dias.forEach((offset, i) => insertar.run(
    TEMAS[i][0], TEMAS[i][1], sumarDias(hoy, offset), '23:59', SEDES[i].sedeNombre, SEDES[i].sedeDireccion));

  POSTULANTES.forEach((p, i) => repo.insertar({
    fechaNacimiento: '1990-05-1' + i,
    domicilio: `Calle de Ejemplo ${100 + i}`,
    telefono: '1155551234',
    email: `${p.nombre.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '')}@example.com`,
    partido: null,
    fechaRegistro: reloj(),
    ...p,
  }));
  return true;
}
