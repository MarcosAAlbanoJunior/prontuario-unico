# CLAUDE.md — prontuario-unico

Self-hosted, single-user personal health record: Telegram bot + web dashboard + Spring Boot backend compiled to a GraalVM native image.
The source of truth for scope and behavior is `docs/SPEC-MVP.md`. Read the relevant section before any task. If a request conflicts with the spec, stop and ask.

## Golden rules

- Work one milestone (or one task inside it) at a time. Start with a short plan, wait for approval, then implement.
- Language: code, identifiers, DB, API, commits, README, ADRs and comments in **English**. Everything the user sees (bot messages, dashboard, labels, LLM-generated `concept`) in **pt-BR**, always through the message files, never hardcoded.
- Versions: always the latest **stable/LTS**. Check the official site before adding or upgrading anything. Never add a dependency without asking first and saying why.
- Not a diagnosis tool: never write code or copy that suggests diagnoses or treatments.
- Ask before: new dependency, schema change outside a new Flyway migration, changing a public API contract, deleting files.

## Stack

Java 25 (LTS) · Spring Boot 4.1.x · GraalVM CE 25 native image · Maven 3.10 · PostgreSQL 18 · Spring Data JDBC + `JdbcClient` · Flyway · JUnit + Testcontainers + ArchUnit · React 19 + Vite 8 + TypeScript (strict) · Node LTS.

## Commands

```bash
./mvnw -f backend verify                    # unit + integration tests (JVM)
./mvnw -f backend spring-boot:run           # run on the JVM (daily dev)
./mvnw -f backend -Pnative native:test      # tests on the native binary (slow; the private CI runs it)
scripts/verify.sh                           # every check a change must pass (backend + frontend)
npm --prefix frontend run dev               # Vite dev server, proxies /api to :8080
npm --prefix frontend run lint && npm --prefix frontend test
docker compose -f deploy/docker-compose.yml up -d
```

Do not run the native build locally unless asked: it takes minutes and several GB of RAM.
CI/CD lives in the maintainer's private repository, not here: do not add GitHub Actions workflows. Run `scripts/verify.sh` before committing.

## Backend architecture: hexagonal, package by feature

Each feature module (`telegram`, `entries`, `exams`, `appointments`, `goals`, `llm`, `dashboard`) has the same layout:

```text
io.github.<user>.prontuario.<module>/
├── domain/                 entities, value objects, domain services, domain exceptions — pure Java
├── application/
│   ├── port/in/            use case interfaces + command/query records
│   ├── port/out/           interfaces the module needs (repositories, gateways, clock, notifier)
│   └── service/            use case implementations
├── adapter/
│   ├── in/web/             REST controllers, request/response DTOs, mappers
│   ├── in/telegram/        update handlers (telegram module only)
│   └── out/persistence/    Spring Data JDBC rows, repositories, mappers
│   └── out/<other>/        HTTP clients (Telegram API, LLM), file storage
└── config/                 Spring wiring for this module (@Configuration, @Bean)
common/                     shared kernel only: ids, time, i18n, error model. Keep it small.
```

Dependency rules (enforced by ArchUnit tests in `backend/src/test/.../architecture`):

- `domain` depends on nothing but the JDK. No Spring, no Jackson, no JDBC annotations.
- `application` depends only on `domain` and its own ports. No Spring imports; wiring and transactions live in `config` (`TransactionTemplate` wrapper or a transactional decorator).
- `adapter` depends on `application` ports, never the reverse. Adapters never call other adapters.
- Modules talk to each other **only** through the other module's `port/in` interfaces. No access to another module's `domain`, `service` or `adapter` classes.
- Persistence rows (`*Row`) and web DTOs (`*Request`, `*Response`) never leave their adapter. Map to domain at the boundary.
- No cycles between modules.

## Code conventions

- SOLID and small units: a class has one reason to change; methods short enough to read without scrolling; extract when a name explains intent better than a comment.
- Hard limit: no source file in `src/main/java` over 200 lines (enforced by `SourceFileSizeTest`). When a class grows, split it by responsibility or sub-context instead of compressing code.
- Names over comments. Comments explain *why*, never *what*.
- Immutability by default: `record` for value objects, commands, queries and DTOs; `final` fields; no setters in the domain.
- Domain invariants are checked in constructors/factory methods and throw domain exceptions. Bean Validation only at the web edge.
- No `null` in public APIs: return `Optional` for "maybe absent", empty collections for "none".
- Constructor injection only. No field injection, no `@Autowired`.
- No Lombok (records cover it and it keeps native image predictable).
- Time: always inject `java.time.Clock`; store `Instant`/`timestamptz`; convert to `APP_TIMEZONE` only at the edges.
- Errors: domain exceptions mapped to `application/problem+json` in a single `@RestControllerAdvice`.
- Logs: structured, English, ids and durations only. **Never log message text, symptom content or lab values.**
- SQL: explicit and readable; every schema change is a new Flyway migration (`V<n>__description.sql`), never edit an applied one.

## GraalVM native image constraints

- Avoid libraries that rely on runtime reflection, dynamic proxies or classpath scanning without reachability metadata.
- JSON records used by Jackson/`RestClient` (Telegram, LLM, API) need `@RegisterReflectionForBinding` or a `RuntimeHintsRegistrar`.
- Telegram and LLM clients use plain `RestClient` with records. No bot SDK, no Spring AI.
- When adding anything that touches reflection, resources or serialization, make sure `native:test` covers it.

## Testing

- Write the test with the change. Test-first for domain and use cases.
- `domain`: plain unit tests, no Spring context.
- `application/service`: unit tests with in-memory fakes of `port/out` (prefer fakes over mocks).
- `adapter/out/persistence`: integration tests with Testcontainers (PostgreSQL 18).
- `adapter/in/web`: slice tests (`@WebMvcTest`) for status codes, validation and the problem+json format.
- ArchUnit tests guard the dependency rules above; run on the JVM only.
- Test names describe behavior: `shouldRejectEventEndingBeforeStart`.
- Deterministic rules (goal progress, body-point state, lab status, dedupe, command parsing) need table-driven tests covering the edge cases listed in the spec.

## Frontend

- TypeScript `strict`, function components and hooks, folders by feature (`overview/`, `timeline/`, `exams/`, `goals/`).
- All user-facing strings in `src/i18n/pt-BR.ts`; format numbers and dates with `Intl` in `pt-BR`.
- API client generated from the backend OpenAPI; do not hand-write fetch calls for existing endpoints.
- Accessible by default: real `<button>`/`<a>`/`<label>`, visible focus, color never the only signal (body-point states always show the reason).
- Follow the design in the canvas (body map in the center, goals on the left, detail on the right); mobile layout stacks the columns.

## Security (defaults the code must keep)

- App refuses to start without `APP_PASSWORD_HASH`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_ALLOWED_CHAT_ID`. No default credentials, no demo user.
- Only the allowed Telegram chat is served. Bind to `127.0.0.1`. No telemetry, no outbound calls except Telegram and the configured LLM.
- Session cookie `HttpOnly` + `SameSite=Strict` (+ `Secure` behind HTTPS), CSRF on, login rate limit, strict CSP.
- Uploads: check magic bytes, 20 MB max, store as sha256, authenticated download only.
- Never commit secrets; `.env` is gitignored, `.env.example` has placeholders only.

## Git

- Conventional Commits in English (`feat(entries): parse /sono command`), small and focused.
- Linear history: rebase, never merge. Use `git pull --rebase`; no merge commits.
- During the MVP, commit directly on `main`: no feature branches and no PRs. Each task (or a coherent step inside it) becomes one or more commits; the commit message says what changed and how it was tested when that is not obvious.
- Significant decisions get a short ADR in `docs/adr/` (context, decision, consequences).