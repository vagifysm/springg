# Spring Boot Backend Starter Template

A reusable backend foundation: **infrastructure, security and conventions are already in place**, so a new
project only needs entities, DTOs, services and controllers.

> **Status note:** this template was generated in an environment without network, Docker or Gradle.
> Nothing in it has been compiled or run yet. Follow [First run & verification](#first-run--verification)
> to confirm it on your machine and fix anything that surfaces (e.g. a dependency version).

## Technologies

Java 21 · Spring Boot 3.5 · Gradle (Groovy DSL) · PostgreSQL 16 · Spring Data JPA · Liquibase ·
Spring Security + JWT (jjwt) · Bean Validation · MapStruct · Lombok · springdoc OpenAPI (Swagger UI) ·
Actuator · Docker / Docker Compose · JUnit 5 · Mockito · Testcontainers · DevTools

## Architecture

```
src/main/java/com/example/app/
├── Application.java
├── config/        Properties records, CORS, JPA auditing, OpenAPI, request-id filter, admin bootstrap
├── constant/      ApiPaths, SecurityConstants (public endpoints, token constants)
├── controller/    Thin REST controllers: HTTP only, no business logic
├── dto/
│   ├── request/   Inbound payloads + validation annotations
│   └── response/  Outbound payloads (ApiResponse envelope, PageResponse, ...)
├── entity/        JPA entities (extend BaseEntity: UUID id + audit columns)
├── enums/         Role (USER, ADMIN)
├── exception/     ApiException hierarchy + GlobalExceptionHandler
├── mapper/        MapStruct mappers (entity <-> DTO)
├── repository/    Spring Data repositories
├── security/      JWT service/filter, SecurityConfig, UserDetails adapter
├── service/       Service interfaces
│   └── impl/      Business logic + transactions
└── util/          Small static helpers
```

Request flow: `Controller → Service (transaction, business rules) → Repository → PostgreSQL`.
Entities never leave the service layer: controllers receive and return DTOs; MapStruct converts.

Every response uses one envelope:

```json
{ "success": true, "message": "Product created", "data": { }, "timestamp": "2026-01-01T10:00:00Z" }
{ "success": false, "message": "Validation failed", "errors": [{ "field": "price", "message": "must be greater than or equal to 0.00" }], "timestamp": "..." }
```

## Environment variables

Nothing sensitive is hardcoded. Configuration comes from environment variables, or from a git-ignored `.env`
file (Docker Compose reads it automatically; the app also imports `./.env` when run locally).

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | yes | – | PostgreSQL credentials |
| `JWT_SECRET` | yes | – | Base64 HMAC key, ≥ 32 bytes. `openssl rand -base64 48` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | no | `localhost` / `5432` / `appdb` | Database location |
| `JWT_EXPIRATION` | no | `60m` | Access-token lifetime (`30m`, `2h`, ...) |
| `SPRING_PROFILES_ACTIVE` | no | `dev` | `dev` or `prod` |
| `SERVER_PORT` | no | `8080` | HTTP port |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:3000` | Comma-separated origins |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_FULL_NAME` | no | empty | Creates the first ADMIN at startup |
| `SWAGGER_ENABLED` | no | `true` (dev) / `false` (prod) | Toggle Swagger UI + OpenAPI docs |
| `JPA_DDL_AUTO` | no | `none` | Set `validate` to make Hibernate check entities against the Liquibase schema |

Quick setup (generates a random DB password, JWT secret and admin password into `.env`):

```bash
./scripts/init-env.sh
```

Or manually: `cp .env.example .env` and replace every `change-me` value. Do not wrap values in quotes.

## Run with Docker (everything)

```bash
./scripts/init-env.sh          # once
docker compose up --build
```

Starts PostgreSQL and the app. Liquibase migrates the schema on startup.
Stop with `docker compose down` (add `-v` to also delete the database volume).

## Run locally (IDE or Gradle)

Requires JDK 21. Start only the database in Docker, then run the app:

```bash
docker compose up -d postgres
gradle bootRun                 # or ./gradlew bootRun once the wrapper exists (see below)
```

`DB_HOST=localhost` in `.env` is picked up automatically. DevTools gives automatic restarts in the `dev` profile.

### Gradle wrapper

The wrapper files could not be generated in the authoring environment. With Gradle ≥ 8.14 installed, run once:

```bash
gradle wrapper --gradle-version 8.14.3
```

then commit `gradlew`, `gradlew.bat` and `gradle/wrapper/`. (The Docker build uses a Gradle image and does not need them.)

## First run & verification

```bash
./gradlew clean build          # compiles, runs unit tests + Testcontainers integration tests (needs Docker)
docker compose up --build
curl -s localhost:8080/actuator/health          # {"status":"UP",...}
```

## Swagger UI

<http://localhost:8080/swagger-ui/index.html> (disabled by default in `prod`, set `SWAGGER_ENABLED=true` to enable).

To test secured endpoints: call `POST /api/v1/auth/login`, copy `data.accessToken`, click **Authorize**
and paste the token (without `Bearer `). The authorization is remembered across page reloads.

## How JWT authentication works

1. `POST /api/v1/auth/register` creates a `USER` (public registration can never create an admin) and returns a token.
   `POST /api/v1/auth/login` verifies email + BCrypt password and returns a token.
2. The token is an HMAC-signed JWT (subject = email, `role` claim, issuer, expiry) built by `JwtService`.
3. Clients send `Authorization: Bearer <token>`. `JwtAuthenticationFilter` validates signature, issuer and expiry,
   loads the user (so disabled/deleted users are rejected immediately) and fills the `SecurityContext`.
4. `SecurityConfig` is stateless (no sessions, no CSRF). Public endpoints are listed in `SecurityConstants.PUBLIC_ENDPOINTS`;
   everything else needs a valid token. Role rules use `@PreAuthorize("hasRole('ADMIN')")` (see `ProductController`).
5. Missing/invalid token → `401`, insufficient role → `403`, both as the standard JSON envelope.

The first ADMIN comes from `ADMIN_EMAIL` / `ADMIN_PASSWORD` (created at startup if missing).

## Example module: Products

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/products` | ADMIN |
| GET | `/api/v1/products/{id}` | any authenticated user |
| GET | `/api/v1/products?page=0&size=20&sort=price,desc` | any authenticated user |
| PUT | `/api/v1/products/{id}` | ADMIN |
| DELETE | `/api/v1/products/{id}` | ADMIN |

Page size is capped at 100 (`spring.data.web.pageable.max-page-size`). Unknown sort properties return `400`.

## How to create a new entity / CRUD module

Example: `Customer`. Copy the Product module and rename; the steps are:

1. **Migration** – add `src/main/resources/db/changelog/changes/003-create-customers-table.sql`
   (UUID primary key, audit columns, constraints) and register it in `db.changelog-master.yaml`.
2. **Entity** – `entity/Customer.java` extending `BaseEntity`, `@Entity @Table(name = "customers")`, Lombok `@Getter @Setter @NoArgsConstructor`.
3. **Repository** – `interface CustomerRepository extends JpaRepository<Customer, UUID>`.
4. **DTOs** – `CreateCustomerRequest`, `UpdateCustomerRequest` (records + validation), `CustomerResponse` (record).
5. **Mapper** – `@Mapper(config = MapStructConfig.class)` with `toEntity`, `toResponse`, `updateEntity`.
   Unmapped target fields are a compile error by design: ignore `id` and audit fields explicitly, as `ProductMapper` does.
6. **Service** – `CustomerService` interface + `impl/CustomerServiceImpl` (`@Transactional`, throw `ResourceNotFoundException` / `ConflictException`).
7. **Controller** – `CustomerController` under `ApiPaths` (add a constant), return `ApiResponse<...>`, protect with `@PreAuthorize` where needed.
8. **Tests** – a Mockito unit test for the service and an integration test extending `AbstractIntegrationTest`.

## How to add Liquibase migrations

Never edit a migration that has been applied anywhere. Always add a new file:

```sql
--liquibase formatted sql

--changeset yourname:004-add-phone-to-customers
ALTER TABLE customers ADD COLUMN phone VARCHAR(30);
--rollback ALTER TABLE customers DROP COLUMN phone;
```

Save it in `db/changelog/changes/`, add an `include` entry at the end of `db.changelog-master.yaml`, then update the entity.
Migrations run automatically on startup. Tip: set `JPA_DDL_AUTO=validate` to have Hibernate verify entities against the schema.

## How to write tests

- **Unit tests** (`src/test/.../service`, `.../security`): plain JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`). No Spring, no Docker.
- **Integration tests** (`src/test/.../controller`): extend `AbstractIntegrationTest`. It boots the full app against a real
  PostgreSQL started by Testcontainers (Docker must be running), applies the Liquibase migrations, generates a random JWT secret,
  and exposes `MockMvc`, `createUser(...)` and `loginAndGetToken(...)`.
- Run everything: `./gradlew test`.

## Logging

- `dev`: readable console logs including a request id (`[%X{requestId}]`).
- `prod`: structured JSON logs (Elastic Common Schema) via Spring Boot's built-in structured logging; the request id appears as a field.
- Every response carries an `X-Request-Id` header; send your own to correlate across services.

## Actuator

`/actuator/health` and `/actuator/info` are public (liveness/readiness probes: `/actuator/health/liveness`, `/readiness`).
Any other actuator endpoint requires an ADMIN token. Exposure is wider in `dev` (metrics, loggers) than in `prod`.

## Production checklist

- Run with `SPRING_PROFILES_ACTIVE=prod`; keep Swagger off unless you need it.
- Provide `JWT_SECRET` and DB credentials from a secret manager, not from a file in the image.
- Restrict `CORS_ALLOWED_ORIGINS` to your real front-end origins.
- Put TLS in front of the app (reverse proxy / load balancer).
- Consider refresh tokens and token revocation if you need sessions longer than the access-token lifetime.
