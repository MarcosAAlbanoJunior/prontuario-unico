# AGENTS.md — prontuario-unico

Instructions for any coding agent (Claude Code, Codex, Cursor, ...) working in this repository. Read this before writing code.

## 1. What this project is

Self-hosted, single-user personal health record: a Telegram bot to log symptoms and habits in seconds, reminders, and a web dashboard. One Spring Boot backend compiled to a GraalVM native image, plus PostgreSQL. Open source (Apache-2.0); each person runs their own instance on a small VPS or home server.

- **The source of truth for scope and behavior is [docs/SPEC-MVP.md](docs/SPEC-MVP.md).** Read the relevant section before any task. If a request conflicts with the spec, stop and ask.
- **Not a diagnosis tool.** Never write code or copy that suggests diagnoses or treatments. The app shows what the user recorded, with its source.
- **The backend is agnostic to the frontend.** The only contract between them is the HTTP API, described by OpenAPI. Do not shape backend code around screen details.

## 2. Golden rules

- Work one milestone (or one task inside it) at a time. Start with a short plan, wait for approval, then implement.
- Language: code, identifiers, database, API, commits, README, ADRs, comments and test names in **English**. Everything the user sees (bot messages, dashboard, labels, LLM-generated `concept`) in **pt-BR**, always through the message files, never hardcoded.
- Versions: always the latest **stable/LTS**. Check the official site before adding or upgrading anything. Prefer versions managed by the Spring Boot BOM.
- Ask before: adding a dependency (say why), a schema change outside a new Flyway migration, changing a public API contract, deleting files.

## 3. Stack

Java 25 (LTS) · Spring Boot 4.1.x · GraalVM CE 25 native image · Maven 3.10 · PostgreSQL 18 · Spring Data JDBC + `JdbcClient` (no JPA/Hibernate, see ADR 0002) · Flyway · JUnit + Testcontainers + ArchUnit + JaCoCo · Lombok (limited, see 5.3) · React 19 + Vite 8 + TypeScript (strict) · Node LTS.

Use modern Java when it makes code clearer: records for value objects, commands and DTOs; sealed interfaces with pattern matching for closed sets of outcomes; virtual threads for I/O; text blocks for SQL. Not to show off.

## 4. Architecture: hexagonal, layer first

Packages under `io.github.marcosaalbanojunior.prontuario`:

```text
domain
├── model            entities, value objects, enums, domain exceptions (pure Java)
└── service          pure business rules (parsers, calculations)
application
├── port
│   ├── in           use case interfaces (*UseCase) and their request/result records
│   └── out          what use cases need from the outside (*Port)
└── usecase          use case implementations (*Service): orchestrate domain + out ports
adapter
├── in
│   ├── telegram     long polling runner
│   └── web          REST controllers, request/response DTOs, filters
└── out
    ├── persistence  JdbcClient repositories (*RepositoryAdapter), row mappers
    ├── client       HTTP clients (Telegram Bot API, LLM)
    └── ...          other infrastructure (i18n messages, id generation)
config               Spring wiring (@Configuration, @Bean), @ConfigurationProperties, native hints
```

### 4.1 Dependency rules (non-negotiable, enforced by ArchUnit)

- `domain` depends only on the JDK and on `domain`. No Spring, Jackson, JDBC, Lombok or `application`/`adapter` imports. `domain.model` does not depend on `domain.service`. If a domain test needs a Spring context, the layer is wrong.
- `application` depends on `domain` and on its own ports. No Spring, no `jakarta`, no `adapter`, no `config`. Transactions go through `TransactionPort`; time through an injected `java.time.Clock`.
- `adapter` depends on `application` and `domain`, never the reverse. Inbound adapters call `port.in` use cases; outbound adapters implement `port.out`. Adapters never call other adapters and do not read `config`.
- `config` wires everything and is the only place that knows concrete adapters.
- Adding a feature: start with the port in `application.port`, then the use case in `application.usecase`, then the adapters, then the wiring.

### 4.2 Group by context when a folder grows

No layer folder (`domain.model`, `port.in`, `port.out`, `usecase`, `adapter.out.persistence`, ...) becomes a flat list mixing subjects. When **3 or more** classes in the same layer belong to the same context, move them into a subfolder named after the context, and use the same name in every layer where that context has its own subfolder. Contexts today: `entry` (events and sources), `quickentry` (quick commands), `goal`, `telegram`.

- Do not create a subfolder for 1–2 files; a one-file folder is overengineering.
- Context names are lower-case English nouns, identical across layers.
- This is package organization only; the dependency rules above still apply inside subfolders.

### 4.3 Naming

| What | Where | Name |
| --- | --- | --- |
| Inbound port | `application.port.in` | `RecordQuickEntryUseCase` |
| Use case implementation | `application.usecase` | `RecordQuickEntryService` |
| Outbound port | `application.port.out` | `EventRepositoryPort`, `TelegramGatewayPort` |
| Port implementation | `adapter.out.*` | `EventRepositoryAdapter`, `TelegramGatewayAdapter` |
| Row mapping | `adapter.out.persistence` | `EventRowMapper` |

No JPA entities exist (ADR 0002); persistence adapters use `JdbcClient` with explicit SQL and explicit row mappers (no reflection, native-friendly).

## 5. Code conventions

### 5.1 Clean code

- SOLID and small units: a class has one reason to change; methods short enough to read without scrolling; extract when a name explains intent better than a comment.
- **Hard limit: no source file in `src/main/java` over 200 lines** (enforced by `SourceFileSizeTest`). When a class grows, split it by responsibility or context instead of compressing code.
- Immutability by default: `record` for value objects, commands, results and DTOs; `final` fields; no setters in the domain. Expose behavior, not state.
- No business logic in controllers, bot handlers or repositories: they translate between the outside world and the ports.
- No `null` in domain or application APIs: `Optional` for "maybe absent", empty collections for "none", sealed types for closed sets of outcomes.
- Specific domain exceptions (`InvalidEventException`), never a bare `RuntimeException` hiding the reason. Invariants are checked in constructors and factories.
- Input validation at the edges (Bean Validation on web DTOs), not spread across the domain.
- Names over comments. Comments explain *why* (a non-obvious decision, an external limitation), never *what*.
- Time: inject `java.time.Clock`; store `Instant`/`timestamptz`; convert to `APP_TIMEZONE` only at the edges.
- SQL: explicit and readable. Every schema change is a new Flyway migration `V<n>__short_description.sql`; never edit an applied one.

### 5.2 Web and messages

- Controllers do not return `ResponseEntity`: return the DTO (or `void`) and set a non-200 status with `@ResponseStatus` on the method. Errors are mapped to `application/problem+json` in a single `@RestControllerAdvice`, using `@ResponseStatus` per handler when the status is fixed.
- Message keys (`messages_pt_BR.properties`, `frontend/src/i18n`): words inside a segment are separated by `-` (kebab-case); `.` only separates groups. `telegram.nothing-to-undo`, `body-region.right-knee` — never `nothing_to_undo` or `nothingToUndo`.

### 5.3 Lombok (limited)

Allowed only outside `domain`: `@Slf4j` for loggers and `@RequiredArgsConstructor` for use cases, adapters and configuration classes whose constructor only assigns fields. Never in `domain` (records cover it), no `@Data`, `@Setter`, `@Builder` or `@SneakyThrows`. Constructor injection only; never `@Autowired` or field injection.

## 6. Performance on a small server

The app runs on a small VPS next to other services: memory and CPU are limited, but the data is one person's.

- Know the complexity of what you choose (`List.contains` is O(n); use `Set`/`Map` for repeated lookups). Comment non-obvious performance choices briefly.
- Let PostgreSQL filter, aggregate and paginate. Never load a whole table to process in Java. Any list that can grow (timeline, events, exams) is paginated (cursor or limit).
- Avoid N+1 queries: one query with a join or an aggregate instead of a query per item.
- Virtual threads for I/O (database, Telegram, LLM), never for CPU-bound work.
- Caches only with an explicit size limit and TTL; an unbounded cache is a memory leak.
- Prefer the simple, readable solution over a theoretically faster complex one unless a measurement shows the need. Measure before optimizing.

## 7. GraalVM native image

- Avoid libraries that rely on runtime reflection, dynamic proxies or classpath scanning without reachability metadata.
- JSON records used by Jackson/`RestClient` (Telegram, LLM, API) need a `RuntimeHintsRegistrar` (binding hints); classpath resources need resource hints.
- Telegram and LLM clients use plain `RestClient` with records. No bot SDK, no Spring AI.
- Settings that switch behavior at runtime are read at runtime, not with `@Conditional*`, which the native build evaluates once at build time.
- When adding anything that touches reflection, resources or serialization, make sure `native:test` covers it.

## 8. Testing — 100% coverage

- Every change comes with its tests. Test-first for domain rules and use cases.
- **Coverage: 100% of lines and branches**, enforced by JaCoCo in `./mvnw -f backend verify`. Only the `main` class and the `config` package (wiring, exercised by context tests) are excluded. If something is genuinely untestable, isolate it in a minimal piece and document why; never let coverage drop silently.
- `domain`: plain unit tests, no Spring, no mocks.
- `application.usecase`: unit tests with in-memory fakes or mocks of `port.out` interfaces only; never reimplement domain logic in a test double.
- `adapter.out.persistence`: integration tests with Testcontainers (PostgreSQL 18), never H2.
- `adapter.out.client`: `MockRestServiceServer` or a local stub server.
- `adapter.in.web`: `@WebMvcTest` slices for status codes, validation and problem+json.
- End-to-end flows: `@SpringBootTest` with Testcontainers and stubbed external services.
- ArchUnit guards the rules in section 4; it and the file size test run on the JVM only (`@DisabledInNativeImage`).

### 8.1 Structure and names

- Every test class has a `@DisplayName` saying what is under test, in English.
- Group by method or behavior with `@Nested` classes when a class tests more than one method or clearly distinct scenarios; do not nest artificially for 1–2 tests.
- Every `@Nested` class and every test has a `@DisplayName` describing scenario and expected result (`"rejects an event that ends before it starts"`). Method names follow `should_<result>_when_<condition>` for navigation and stack traces.
- Use `@ParameterizedTest` from **3 equivalent variations** of the same scenario (`@MethodSource` for objects, `@CsvSource`/`@ValueSource`/`@EnumSource` for simple values) with a descriptive `name`. Do not parametrize scenarios that are not really equivalent.
- Deterministic rules (goal progress, body-point state, lab status, dedupe, command parsing) need table-driven tests covering the edge cases listed in the spec.

## 9. Logs — traceability and LGPD

**Logs leave the application's control** (files, disk, possibly an aggregator). Health data is *sensitive personal data* under LGPD (art. 11), so the rule is stricter than usual: anyone with access to the server must be able to read the logs without that becoming an incident. Traceability comes from trace ids and internal ids, never from personal data.

### 9.1 Trace id

- Every HTTP request gets a `traceId`: reuse a valid `X-Request-Id` header or generate a UUID; put it in the MDC, echo it in the `X-Request-Id` response header, and always remove it in a `finally` block (threads are reused).
- Every Telegram update is handled with its own `traceId` in the MDC, removed when the update is done.
- Work handed to another thread copies the MDC explicitly.

### 9.2 Never log, at any level

- Message text, symptoms, concepts, body regions, lab values, notes, file names or contents.
- Names, phone numbers, e-mails, **Telegram chat ids or user ids**, or any external identifier of a person.
- Passwords or hashes, the bot token (it is part of the Bot API URL, so HTTP errors are logged without their cause), API keys, session ids.
- Raw request/response bodies, and `toString()` of domain objects, records or DTOs.
- Exception messages that may embed user input.

### 9.3 Safe and sufficient

- Internal ids (event, source and update UUIDs; Telegram `update_id`), error codes, enum names, field names that failed validation, HTTP status, counts, durations, booleans.
- Business events as ids only: `log.info("Event recorded. eventId={}", event.id())`.

### 9.4 Levels and format

- Declare loggers with Lombok `@Slf4j`.
- `ERROR` unexpected failures, with the stack trace; `WARN` expected abnormal conditions; `INFO` business events (ids only); `DEBUG` local investigation, off in production.
- Production logs are structured (ECS JSON via Spring Boot structured logging), and the MDC `traceId` is included automatically. Development logs are plain text with `%X{traceId}`.

## 10. Frontend

- TypeScript `strict`, function components and hooks, folders by feature (`overview/`, `timeline/`, `exams/`, `goals/`).
- All user-facing strings in `src/i18n/pt-BR.ts`; format numbers and dates with `Intl` in `pt-BR`.
- API client generated from the backend OpenAPI; do not hand-write fetch calls for existing endpoints.
- Accessible by default: real `<button>`/`<a>`/`<label>`, visible focus, color never the only signal (body-point states always show the reason).
- Follow the design (body map in the center, goals on the left, detail on the right); mobile stacks the columns.

## 11. Security defaults the code must keep

- The app refuses to start without `APP_PASSWORD_HASH`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_ALLOWED_CHAT_ID`. No default credentials, no demo user.
- Only the allowed Telegram chat is served; any other chat only gets its own chat id in reply to `/start`. Bind to `127.0.0.1`. No telemetry, no outbound calls except Telegram and the configured LLM.
- Session cookie `HttpOnly` + `SameSite=Strict` (+ `Secure` behind HTTPS), CSRF on, login rate limit, strict CSP.
- Uploads: check magic bytes, 20 MB max, store as sha256, authenticated download only.
- Never commit secrets; `.env` is gitignored, `.env.example` has placeholders only.

## 12. Infrastructure and commands

```bash
scripts/verify.sh                           # every check a change must pass (backend + frontend)
./mvnw -f backend verify                    # backend tests, architecture rules, 100% coverage gate
./mvnw -f backend spring-boot:run           # run on the JVM (daily dev)
./mvnw -f backend -Pnative native:test      # tests on the native binary (slow; the private CI runs it)
docker compose -f deploy/compose.dev.yml up -d   # development PostgreSQL on 127.0.0.1:5432
npm --prefix frontend run dev               # Vite dev server, proxies /api to :8080
docker compose -f deploy/docker-compose.yml up -d   # production stack (app + db)
```

- Do not run the native build locally unless asked: it takes minutes and several GB of RAM.
- CI/CD lives in the maintainer's private repository (ADR 0003): do not add GitHub Actions workflows.
- Flyway owns the schema; migrations live in `backend/src/main/resources/db/migration`.

## 13. Git

- Conventional Commits in English (`feat(telegram): ask for the missing amount`), small and focused, saying why.
- Linear history: rebase, never merge. Use `git pull --rebase`; no merge commits.
- During the MVP, commit directly on `main`: no feature branches and no PRs. Never rewrite commits that were already pushed without saying so.
- Significant decisions get a short ADR in `docs/adr/` (context, decision, consequences).

## 14. Definition of done

1. Follows the architecture in section 4 (ArchUnit green, domain free of frameworks).
2. Tests for all new or changed code, all passing, 100% line and branch coverage.
3. `scripts/verify.sh` passes with no avoidable warnings.
4. Names, structure, logs and comments follow sections 4–9.
5. Committed on `main` with a linear history.
