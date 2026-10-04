# Portal para autoridades de mesa

Implementación del documento de requerimientos y casos de uso (TP Ingeniería de Software, Grupo 13).

## Stack
Java 21 · Spring Boot 3 (REST, JPA, validaciones, Security, Mail) · H2 en archivo · frontend HTML/JS servido por Spring Boot.

## Ejecutar
```bash
mvn spring-boot:run   # http://localhost:8080  (público)  |  /admin.html (administrador)
mvn test
```

## Configuración (variables de entorno)
| Variable | Uso |
|---|---|
| `PORTAL_ADMIN_USER` / `PORTAL_ADMIN_PASSWORD` | Credenciales del administrador (RNF-03) |
| `PORTAL_ADMIN_EMAIL` | Casilla que recibe los reportes (RF-07, RF-14) |
| `PORTAL_ELECCION_FECHA` | `AAAA-MM-DD`; toda charla debe ser anterior (RF-01) |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Servidor de correo institucional (RF-15) |
| `PORTAL_CORREO_REMITENTE` | Remitente de los correos |
| `PORTAL_CIFRADO_CLAVE` | Clave AES-256 en Base64 (DNI, domicilio y afiliación cifrados, RNF-04) |

## Requerimientos → código
| Requerimiento | Dónde |
|---|---|
| RF-01 Publicar charlas | `POST /api/charlas` (admin) · `CharlaController` |
| RF-02 Ver charlas, RNF-01 mapa | `GET /api/charlas` · `index.html` |
| RF-03 / RF-04 Habilitar / bloquear inscripción | `ConvocatoriaService.estado()`: abre el día de la primera charla y cierra en fecha y hora de la última |
| RF-05 Registro de postulación | `POST /api/postulantes` · `PostulanteService.registrar` |
| RF-06 / RF-07 / CU1 Reporte de cierre | `ConvocatoriaService.cerrarSiCorresponde` (scheduler cada minuto) |
| RF-08 / CU2 Consultar | `GET /api/postulantes` (solo con convocatoria cerrada) |
| RF-09 / RF-11 / CU3-5 Aprobar / rechazar (motivo obligatorio) | `POST /api/postulantes/{id}/aprobar`, `/rechazar` |
| RF-10 / RF-12 / CU6-7 Correos al postulante | `PostulanteService` → `CorreoService` |
| RF-13 / RF-14 / CU8 Reporte final | `ConvocatoriaService.enviarReporteFinalSiCorresponde` |
| RF-15 Servidor de correo | `CorreoService`: los correos se encolan en `CorreoSaliente` y se reintentan si el servidor falla |
| RNF-02 / RNF-03 | `SecurityConfig` |
| RNF-04 | `CifradoService`, `CampoCifrado` |

## Pendiente de definir (marcado con `*` en el documento)
Validaciones contra fuentes externas que el sistema no puede verificar por sí solo: DNI emitido por RENAPER,
coincidencia de nombre/apellido/fecha con el DNI, agrupación política reconocida y que la dirección de la sede
sea de Argentina.
