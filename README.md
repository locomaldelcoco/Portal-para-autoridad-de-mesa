# Portal para autoridades de mesa

Implementación del documento de requerimientos y casos de uso (TP Ingeniería de Software, Grupo 13).

## Stack
Solo JavaScript, HTML y CSS.
- **Backend**: Node.js ≥ 22.13 con Express. Base de datos SQLite integrada en Node (`node:sqlite`).
- **Frontend**: HTML + JavaScript + CSS puros en `public/`, servidos por el mismo backend.
- **Dependencias**: `express` y `nodemailer` (envío de correos).

## Ejecutar
Requiere **Node.js 22.13 o superior** y conexión a internet (la API de la USIG y el mapa son servicios externos, sin claves).
```bash
npm install      # instala todas las dependencias (una sola vez)
npm start        # http://localhost:3000  (público)  |  /admin.html (administrador: admin / cambiar-esto)
npm test
```
El prototipo trae **datos de ejemplo** (3 charlas y 3 postulantes) que se cargan solos en el primer arranque.

| Comando | Escenario |
|---|---|
| `npm start` | Convocatoria **abierta**: ver charlas, ver el mapa de cada sede e inscribirse |
| `npm run start:cerrada` | Convocatoria **cerrada**: en `/admin.html` consultar, aprobar y rechazar los postulantes |

Cada escenario usa su propia base de datos en `data/`. Para reiniciarlos, detener el servidor y borrar esa carpeta.

## Mapa (API de la USIG)
Al tocar "Ver mapa", el backend envía la dirección de la sede a `servicios.usig.buenosaires.gob.ar/normalizar`
y las coordenadas de la respuesta se usan para mostrar la ubicación (no hay coordenadas cargadas de antemano).
Para probar otra dirección: en `/admin.html` publicar una charla y elegir la dirección entre las sugerencias.

## Configuración (variables de entorno)
| Variable | Uso |
|---|---|
| `PORT` | Puerto (por defecto 3000) |
| `PORTAL_ADMIN_USER` / `PORTAL_ADMIN_PASSWORD` | Credenciales del administrador (RNF-03). Por defecto `admin` / `cambiar-esto` |
| `PORTAL_ADMIN_EMAIL` | Casilla que recibe los reportes (RF-07, RF-14) |
| `PORTAL_ELECCION_FECHA` | `AAAA-MM-DD`; toda charla debe ser anterior (RF-01) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` | Servidor de correo institucional (RF-15) |
| `PORTAL_CORREO_REMITENTE` | Remitente de los correos |
| `PORTAL_CIFRADO_CLAVE` | Clave AES-256 en Base64 (RNF-04). Si no se define, se genera una en `data/clave-cifrado.key` (solo desarrollo) |
| `PORTAL_DB` | Ruta del archivo SQLite (por defecto `data/portal.db`) |

## Requerimientos → código
| Requerimiento | Dónde |
|---|---|
| RF-01 Publicar charlas | `POST /api/charlas` (admin) · `server/charlas.js` |
| RF-02 Ver charlas, RNF-01 mapa | `GET /api/charlas` · `public/index.html` |
| RNF-01 Sugerencias de direcciones al publicar una charla | `GET /api/direcciones?q=` (admin) · `server/direcciones.js` consulta el normalizador de la USIG |
| RF-03 / RF-04 Habilitar / bloquear inscripción | `server/convocatoria.js` `estado()`: abre el día de la primera charla y cierra en fecha y hora de la última |
| RF-05 Registro de postulación | `POST /api/postulantes` · `server/postulantes.js` |
| RF-06 / RF-07 / CU1 Reporte de cierre | `convocatoria.cerrarSiCorresponde` (tarea programada cada minuto) |
| RF-08 / CU2 Consultar | `GET /api/postulantes` (solo con convocatoria cerrada) |
| RF-09 / RF-11 / CU3-5 Aprobar / rechazar (motivo obligatorio) | `POST /api/postulantes/:id/aprobar`, `/rechazar` |
| RF-10 / RF-12 / CU6-7 Correos al postulante | `server/postulantes.js` → `server/correo.js` |
| RF-13 / RF-14 / CU8 Reporte final | `convocatoria.enviarReporteFinalSiCorresponde` |
| RF-15 Servidor de correo | `server/correo.js`: los correos se encolan en la tabla `correo` y se reintentan si el servidor falla |
| RNF-02 / RNF-03 | `server/app.js` (rutas públicas y `soloAdmin`) |
| RNF-04 | `server/cifrado.js` |

## Pendiente de definir (marcado con `*` en el documento)
Validaciones contra fuentes externas que el sistema no puede verificar por sí solo: DNI emitido por RENAPER,
coincidencia de nombre/apellido/fecha con el DNI, agrupación política reconocida y que la dirección de la sede
sea de Argentina.
