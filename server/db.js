import fs from 'node:fs';
import path from 'node:path';
import { DatabaseSync } from 'node:sqlite';

const ESQUEMA = `
CREATE TABLE IF NOT EXISTS charla (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL, tema TEXT NOT NULL,
  fecha TEXT NOT NULL, horario TEXT NOT NULL,
  sede_nombre TEXT NOT NULL, sede_direccion TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS convocatoria (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  cierre_procesado INTEGER NOT NULL DEFAULT 0,
  reporte_final_encolado INTEGER NOT NULL DEFAULT 0
);
INSERT OR IGNORE INTO convocatoria (id) VALUES (1);
CREATE TABLE IF NOT EXISTS postulante (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  distrito_electoral TEXT NOT NULL, nombre TEXT NOT NULL, apellido TEXT NOT NULL,
  dni TEXT NOT NULL,                -- cifrado
  dni_hash TEXT NOT NULL UNIQUE,    -- para detectar repetidos sin descifrar
  fecha_nacimiento TEXT NOT NULL,
  domicilio TEXT NOT NULL,          -- cifrado
  telefono TEXT NOT NULL, email TEXT NOT NULL,
  antecedentes_mesa INTEGER NOT NULL, estado_capacitacion TEXT NOT NULL,
  afiliado INTEGER NOT NULL,
  partido TEXT,                     -- cifrado
  charla_interes_id INTEGER,
  estado TEXT NOT NULL DEFAULT 'PENDIENTE',
  motivo_rechazo TEXT,
  fecha_registro TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS correo (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  destinatario TEXT NOT NULL, asunto TEXT NOT NULL, cuerpo TEXT NOT NULL,
  estado TEXT NOT NULL DEFAULT 'PENDIENTE', intentos INTEGER NOT NULL DEFAULT 0,
  ultimo_error TEXT, creado TEXT NOT NULL
);`;

export function abrirDb(ruta) {
  if (ruta !== ':memory:') fs.mkdirSync(path.dirname(ruta), { recursive: true });
  const db = new DatabaseSync(ruta);
  db.exec(ESQUEMA);
  return db;
}

/** Ejecuta `fn` en una transacción: si lanza un error, se revierte todo (incluidos los correos encolados). */
export function transaccion(db, fn) {
  db.exec('BEGIN');
  try {
    const resultado = fn();
    db.exec('COMMIT');
    return resultado;
  } catch (e) {
    db.exec('ROLLBACK');
    throw e;
  }
}
