# SONE Messaging Service

Microservicio de mensajería de la plataforma **SONE**. Permite conversaciones entre usuarios y partners del marketplace, con mensajes de texto e imágenes, historial paginado y entrega en tiempo real por WebSocket.

## Funcionalidades

- Creación u obtención de conversaciones, validando que el partner esté activo (consulta al Marketplace).
- Envío de mensajes de texto e imágenes (máx. 5 MB por archivo).
- Límite diario de mensajes.
- Historial de mensajes paginado.
- Notificaciones en tiempo real (STOMP sobre WebSocket). Ver [docs/websocket.md](docs/websocket.md).
- Autenticación con JWT (clave pública RSA).

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Validation, Actuator) |
| Persistencia | Spring Data JPA / Hibernate, PostgreSQL 15 |
| Migraciones | Flyway |
| Seguridad | Spring Security, OAuth2 Resource Server (JWT) |
| Tiempo real | Spring WebSocket (STOMP) |
| Documentación | springdoc-openapi (Swagger UI) |
| Build | Maven (Wrapper) |
| Infraestructura local | Docker Compose |
| Pruebas | JUnit, Spring Boot Test, Spring Security Test |

## Arquitectura

Arquitectura hexagonal por módulos (`conversation`, `message`, `shared`), con capas `domain`, `application` (casos de uso y puertos) e `infrastructure` (web, JPA, adaptadores externos).

## Puesta en marcha

Requisitos: Java 21 y Docker.

```bash
docker compose up -d      # PostgreSQL en el puerto 5433
./mvnw spring-boot:run    # requiere keys/public.pem
./mvnw test
```

## Configuración (variables de entorno)

| Variable | Default |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5433` / `sone_messaging` |
| `DB_USERNAME` / `DB_PASSWORD` | `sone_messaging` / `sone_messaging` |
| `JWT_PUBLIC_KEY_LOCATION` | `file:./keys/public.pem` |
| `MARKETPLACE_BASE_URL` | `http://localhost:3001` |
| `STORAGE_PROVIDER` | `mock` (S3 pendiente) |
| `WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` | `*` (restringir en producción) |

## API

Swagger UI en `/api/docs` y OpenAPI en `/v3/api-docs`.
