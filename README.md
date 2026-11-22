# HU-01 · Microservicio CRUD de Productos con MongoDB

Microservicio RESTful desarrollado con **Spring Boot 3.4** que realiza operaciones CRUD sobre la entidad **Producto** almacenada en **MongoDB**, con validación de datos, excepciones personalizadas, manejador global de errores, pruebas unitarias y de integración, cobertura con **JaCoCo** y análisis estático con **SonarQube**.

| | |
|---|---|
| **Autor** | Jesus Gerardo Ojeda Martinez — IDGS81N, UTCH |
| **Stack** | Java 17+ · Spring Boot 3.4.5 · Spring Web · Spring Data MongoDB · Bean Validation · springdoc-openapi (Swagger UI) |
| **Pruebas** | JUnit 5 · Mockito · MockMvc · MongoDB embebido (Flapdoodle) |
| **Calidad** | JaCoCo 0.8.12 (mínimo 80 % de líneas y ramas) · SonarQube Community Build |

---

## 1. Requisitos

- **JDK 17 o superior** (SonarQube Community Build necesita JDK 21).
- **MongoDB** escuchando en `localhost:27017` para ejecutar la aplicación (las pruebas no lo necesitan: usan MongoDB embebido).
  - Alternativa con Docker: `docker compose up -d mongo`
- Maven no es necesario: el proyecto incluye **Maven Wrapper** (`mvnw` / `mvnw.cmd`).
- Opcional: Node.js (para ejecutar la colección de Postman con Newman).

## 2. Ejecución rápida (Windows)

| Script | Qué hace |
|---|---|
| `1-probar-y-cobertura.bat` | `mvnw clean verify`: pruebas unitarias + integración, reporte JaCoCo y verificación de cobertura ≥ 80 %. |
| `2-levantar-api-y-evidencias.bat` | Arranca la API en `http://localhost:8080`, ejecuta la colección de Postman con Newman y guarda evidencias en `docs/evidencias` y `logs/http`. |
| `3-analisis-sonarqube.bat` | Instala (la primera vez) y arranca SonarQube local, genera un token, ejecuta el análisis y descarga los resultados del Quality Gate. |

## 3. Comandos manuales

```bash
# Pruebas + reporte de cobertura (target/site/jacoco/index.html) + verificación del 80 %
./mvnw clean verify

# Ejecutar la aplicación (requiere MongoDB en localhost:27017)
./mvnw spring-boot:run

# Variables opcionales: MONGO_HOST, MONGO_PORT, MONGO_DB, PORT
MONGO_DB=inventario PORT=9090 ./mvnw spring-boot:run

# Análisis en SonarQube (servidor en http://localhost:9000)
./mvnw clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<TOKEN>
```

Documentación interactiva: **http://localhost:8080/swagger-ui.html** · Especificación OpenAPI: `http://localhost:8080/v3/api-docs`.

## 4. Endpoints

Base: `/api/v1/productos`

| Método | Ruta | Descripción | Éxito | Errores |
|---|---|---|---|---|
| POST | `/api/v1/productos` | Crea un producto | 201 + `Location` | 400, 409 |
| GET | `/api/v1/productos?categoria=` | Lista (filtro opcional por categoría, sin distinguir mayúsculas) | 200 | — |
| GET | `/api/v1/productos/{id}` | Consulta por id | 200 | 404 |
| PUT | `/api/v1/productos/{id}` | Actualiza | 200 | 400, 404, 409 |
| DELETE | `/api/v1/productos/{id}` | Elimina | 204 | 404 |

Ejemplo de solicitud:

```json
{
  "codigo": "ELE-0001",
  "nombre": "Multímetro digital",
  "descripcion": "Medición de voltaje AC/DC",
  "categoria": "Electrónica",
  "precio": 499.90,
  "stock": 25,
  "activo": true
}
```

### Reglas de validación

| Campo | Regla |
|---|---|
| `codigo` | Obligatorio, formato `AAA-0000`, **único** (índice único en MongoDB) |
| `nombre` | Obligatorio, 3 a 80 caracteres |
| `descripcion` | Opcional, máximo 255 caracteres |
| `categoria` | Obligatoria, máximo 40 caracteres |
| `precio` | Obligatorio, 0.01 a 999,999.99, máximo 2 decimales |
| `stock` | Obligatorio, 0 a 100,000 |
| `activo` | Opcional (por defecto `true`) |

### Formato de error

Todas las excepciones se transforman en el mismo JSON (`GlobalExceptionHandler`, `@RestControllerAdvice`):

```json
{
  "timestamp": "2026-11-22T20:15:03.512Z",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "La solicitud contiene datos inválidos",
  "ruta": "/api/v1/productos",
  "detalles": {
    "codigo": "El código debe tener el formato AAA-0000 (ej. ELE-0001)",
    "precio": "El precio debe ser mayor a 0"
  }
}
```

| Excepción | HTTP |
|---|---|
| `MethodArgumentNotValidException` (validación) | 400 |
| `HttpMessageNotReadableException` (JSON mal formado / tipo incorrecto) | 400 |
| `RecursoNoEncontradoException` · ruta inexistente | 404 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `DatoDuplicadoException` · `DuplicateKeyException` (índice único) | 409 |
| Cualquier otra (`Exception`) — sin exponer detalles internos | 500 |

## 5. Arquitectura

```
Cliente HTTP ──JSON──► ProductoController (@Valid, códigos HTTP)
                             │ ProductoRequest / ProductoResponse (DTO)
                             ▼
                       ProductoService (reglas de negocio, unicidad, 404)
                             │ Producto (@Document)
                             ▼
                       ProductoRepository (MongoRepository) ──► MongoDB · colección "productos"

GlobalExceptionHandler (@RestControllerAdvice) ── intercepta las excepciones de todas las capas
```

```
src/main/java/mx/edu/utch/productos
├── ProductosApplication.java
├── config/AppConfig.java              (Clock inyectable, metadatos OpenAPI)
├── controller/ProductoController.java
├── dto/ProductoRequest.java · ProductoResponse.java
├── exception/RecursoNoEncontradoException · DatoDuplicadoException · ErrorResponse · GlobalExceptionHandler
├── model/Producto.java
├── repository/ProductoRepository.java
└── service/ProductoService.java
src/test/java/mx/edu/utch/productos
├── service/ProductoServiceTest.java          (unitarias, Mockito)
├── controller/ProductoControllerTest.java    (capa web + manejador de errores, @WebMvcTest + @MockitoBean)
└── integration/ProductoIntegrationTest.java  (@SpringBootTest + MockMvc + MongoDB embebido)
```

## 6. Pruebas y cobertura

- **Unitarias** (`ProductoServiceTest`): la lógica del servicio con el repositorio simulado por Mockito; escenarios de éxito y error (duplicados, inexistentes, filtros, normalización).
- **Capa web** (`ProductoControllerTest`): `@WebMvcTest` con el servicio simulado; valida códigos HTTP, encabezado `Location`, serialización y cada rama del manejador global (400, 404, 405, 409, 500).
- **Integración** (`ProductoIntegrationTest`): contexto completo de Spring con **MongoDB embebido real**; flujo CRUD de extremo a extremo, filtro por categoría, índice único, validaciones y disponibilidad de OpenAPI.

El reporte de JaCoCo se genera en `target/site/jacoco/index.html`; `mvnw verify` **falla** si la cobertura de líneas o de ramas baja de 80 %.

## 7. Colección de Postman

`postman/HU-01-Productos.postman_collection.json` — importar en Postman (variables `baseUrl` e `id`). También se ejecuta desde consola:

```bash
npx newman run postman/HU-01-Productos.postman_collection.json
```
