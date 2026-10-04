# Portal para autoridad de mesa

Portal con dos funciones:
1. **Charlas informativas**: título, fecha, ubicación (lugar, dirección, localidad) y temas.
2. **Inscripción de ciudadanos** (mayores de 18) para ser autoridad de mesa.

## Stack
- **Backend**: Java 21 + Spring Boot 3 (REST, JPA/Hibernate, Bean Validation, Spring Security)
- **Base de datos**: H2 en archivo (`./data/`), fácil de cambiar por PostgreSQL/MySQL en `application.properties`
- **Frontend**: HTML + JavaScript puro, servido por el mismo Spring Boot (`src/main/resources/static`)

## Ejecutar
```bash
mvn spring-boot:run          # http://localhost:8080
mvn test                     # tests de la API
```
- Portal público: `/` — Panel admin: `/admin.html`
- Credenciales admin por defecto: `admin` / `cambiar-esto`. **Cambiarlas** con las variables
  `PORTAL_ADMIN_USER` y `PORTAL_ADMIN_PASSWORD`.

## API
| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/charlas` (`?todas=true` incluye pasadas), `/api/charlas/{id}` | público |
| POST | `/api/inscripciones` | público |
| POST/PUT/DELETE | `/api/charlas`, `/api/charlas/{id}` | admin |
| GET | `/api/inscripciones` | admin |

## Estructura
```
charla/       entidad, DTO, repositorio y controlador de charlas
inscripcion/  ídem para inscripciones (valida edad y DNI duplicado)
config/       seguridad y manejo de errores
```
