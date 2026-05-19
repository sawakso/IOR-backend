# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
./mvnw clean compile          # compile only
./mvnw clean package          # full jar (skip tests: -DskipTests)

# Run (app starts on port 886)
./mvnw spring-boot:run

# Tests
./mvnw test                                   # all tests
./mvnw test -Dtest=ClassName                  # single class
./mvnw test -Dtest=ClassName#methodName       # single method
```

Configuration is in `src/main/resources/application.yaml`. Secrets go in `.env` (loaded via dotenv-java on startup), with `.env.example` as the template. Database, Redis, mail, and Cloudflare R2 credentials are all `.env`-driven.

## Architecture

**Stack**: Spring Boot 3.5.14, Java 17, MyBatis-Plus 3.5.9, MySQL 8.0, Redis (Lettuce), Spring Security + JWT (jjwt 0.12.5), Knife4j (OpenAPI 3), Cloudflare R2 via AWS S3 SDK.

**Layered architecture** with MyBatis-Plus as the ORM:

```
Controller  →  Service (interface)  →  ServiceImpl (extends ServiceImpl<Mapper, Entity>)
                  ↕                          ↕
              domain/dto                 domain/entity
              domain/vo                  mapper/*
```

- **Controller**: Thin; extracts `userId` from JWT Bearer token, delegates to service, returns `Result<T>`.
- **Service**: Business logic. Service interfaces extend `IService<Entity>`, impls extend `ServiceImpl<Mapper, Entity>` — giving built-in `getById`, `lambdaQuery()`, `save()`, etc.
- **Mapper**: MyBatis-Plus `BaseMapper<Entity>` interfaces. No XML unless custom SQL is needed.
- **domain/dto**: Request bodies (`@Valid` validated). **domain/vo**: Response objects (`Result<T>` wraps them). **domain/entity**: Database entities with `@TableName`, `@TableId`, `@TableLogic`, `@TableField(fill = ...)`.

**Unified response**: `Result<T>` with `code` (200 for success), `message`, and `data`. Static factories: `Result.ok(data)`, `Result.error(code, msg)`, plus `Result.error400/401/403/404/500(msg)`.

**Auth flow**: Stateless JWT. `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) reads `Authorization: Bearer <token>`, validates via `JwtUtil`, and populates `SecurityContextHolder`. `SecurityConfig` disables CSRF, enables CORS (all origins), permits `/user/register`, `/user/login`, `/user/sendcode`, `/user/password/reset`, `/hello`, and all Knife4j paths; everything else requires authentication. `JwtHelper` is a convenience for extracting current userId/role from the request context in services.

**Token lifecycle**: Access tokens expire per `jwt.access-token-expiration` (default 3600s). On sensitive operations (password change, email change, logout), the current token is blacklisted in Redis (`ior:blacklist:token:<token>`) with remaining-TTL expiry. `JwtUtil.validateToken()` checks the blacklist.

**Strategy pattern** is used in two places:
1. `VerificationCodeStrategy` (Redis vs DB storage) — `VerificationCodeContext` picks based on `verify.code.storage` config, defaults to Redis.
2. `UserDeletionStrategy` (per-role deletion logic) — `DeletionStrategyContext` dispatches based on user role.

**Redis caching**: `RedisCacheHelper` implements Cache-Aside: `getOrLoad(key, ttl, loader)` reads cache first, falls back to DB, writes back. Write operations call `invalidate(key)` after DB update. All keys are defined as static methods in `RedisConstants` with format `ior:{module}:{function}:{params}`.

**File storage**: `R2StorageService` wraps AWS S3 SDK targeting Cloudflare R2. Files are stored under folder prefixes (e.g., `avatars/`). URLs use the configured `public-url`.

**Scheduled tasks**: `@EnableScheduling` on the application class. `UserDeletionTask` runs daily at 2 AM, scanning for expired deletion requests (7-day cooldown) and executing them.

## Database Conventions

- Tables prefixed `ior_`, columns `snake_case`, MyBatis-Plus auto-maps to `camelCase` entities.
- **All columns NOT NULL** with sentinel defaults: empty string `''` for VARCHAR, `'1970-01-01 00:00:01'` for DATETIME "not set" markers, `0` for integers.
- Logical deletion via `deleted_at` column with `@TableLogic(value = "'1970-01-01 00:00:01'", delval = "now()")`.
- `created_at` / `updated_at` auto-filled by `MyMetaObjectHandler` (MyBatis-Plus `MetaObjectHandler`).
- Enum-like fields use VARCHAR or TINYINT with COMMENT documenting all values, not MySQL ENUM (except for legacy tables).

## Key Files

| File | Role |
|------|------|
| `config/SecurityConfig.java` | Spring Security rules + JWT filter registration |
| `config/MybatisPlusConfig.java` | Pagination plugin + auto-fill handler |
| `filter/JwtAuthenticationFilter.java` | JWT extraction and SecurityContext population |
| `utils/JwtUtil.java` | JWT generation, validation, blacklist |
| `utils/JwtHelper.java` | Convenience: get current userId/role from request |
| `utils/RedisConstants.java` | All Redis key definitions and TTLs |
| `utils/RedisCacheHelper.java` | Cache-Aside helper (getOrLoad, set, invalidate) |
| `service/R2StorageService.java` | Cloudflare R2 file upload/delete |
| `domain/vo/Result.java` | Unified API response wrapper |
| `task/UserDeletionTask.java` | Scheduled account deletion processing |

## API Docs

Knife4j UI is at `http://localhost:886/doc.html` — accessible without authentication. Controllers use `@Tag` for grouping and `@Operation` for endpoint descriptions.
