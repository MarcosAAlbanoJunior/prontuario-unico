# Contributing

Thanks for your interest. This is a small personal project, so please open an issue to discuss a change before sending a large pull request.

## Ground rules

- Scope and behavior are defined in [docs/SPEC-MVP.md](docs/SPEC-MVP.md). Changes that go beyond it should start as an issue.
- The project never diagnoses or suggests treatments. Contributions that do will not be accepted.
- Code, identifiers, database, API, commits and docs are in **English**. Everything the user sees is in **pt-BR** and lives in the message files (`messages_pt_BR.properties`, `frontend/src/i18n/pt-BR.ts`), never hardcoded.
- New dependencies need a reason in the pull request. Prefer libraries that work in a GraalVM native image without runtime reflection.
- Never log message text, symptoms or lab values. Log ids, types and durations only.

## Development

Requirements: JDK 25, Node.js 24 LTS and Docker (for Testcontainers).

```bash
./mvnw -f backend verify                    # backend unit + integration tests
./mvnw -f backend spring-boot:run           # run the backend on the JVM
npm --prefix frontend run dev               # frontend dev server, proxies /api to :8080
npm --prefix frontend run lint && npm --prefix frontend test
```

The native build is slow and memory hungry; CI runs it on every pull request, so you rarely need it locally.

## Architecture

The backend is a modular monolith with a hexagonal layout per feature module. The dependency rules are enforced by ArchUnit tests. See [CLAUDE.md](CLAUDE.md) for the package layout, conventions and testing strategy.

## Pull requests

1. Create a branch from `main` and keep it up to date with `git rebase`; the history is linear, without merge commits.
2. Write tests with the change. Domain rules need table-driven tests covering the edge cases in the spec.
3. Every schema change is a new Flyway migration; never edit an applied one.
4. Use [Conventional Commits](https://www.conventionalcommits.org/) in English, e.g. `feat(entries): parse /sono command`.
5. Describe in the pull request what changed and how it was tested.

Significant decisions get a short ADR in `docs/adr/` (context, decision, consequences).

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE).
