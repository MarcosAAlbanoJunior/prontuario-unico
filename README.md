# prontuario-unico

A self-hosted, single-user personal health record. Log symptoms and habits from Telegram in seconds, get reminders, and see everything on a web dashboard. Lab results are entered manually with the original PDF kept as the source.

> **Not medical advice.** This project records facts you enter. It does not diagnose, interpret results or suggest treatments, and it does not replace a health professional.

## Status

Early development (milestone M0, foundation). Nothing is installable yet. The scope and the roadmap live in [docs/SPEC-MVP.md](docs/SPEC-MVP.md) (in Portuguese).

## What it will do (v0.1)

- Telegram bot with quick commands (`/agua`, `/sono`, `/treino`, `/peso`, `/dor`) and free text turned into proposed events you confirm, edit or discard.
- Configurable reminders and weekly goals.
- Lab exams with analytes, reference ranges taken from the lab report, and the original PDF attached.
- Appointments and medical guidance with follow-up reminders.
- Web dashboard: body map overview, timeline and exams.
- Full JSON export.

The user interface is in Brazilian Portuguese (pt-BR); code and documentation are in English.

## Design principles

- **One user per instance.** No SaaS, no multi-tenancy.
- **Private by default.** The app only talks to Telegram and, if you enable it, the LLM you configure. It can run fully local with Ollama. No telemetry.
- **Nothing becomes a fact without your confirmation.** The LLM only proposes events; everything else is deterministic code.
- **Small footprint.** A single GraalVM native binary plus PostgreSQL.

## Stack

Java 25 · Spring Boot 4.1 · GraalVM native image · PostgreSQL 18 · Spring Data JDBC · Flyway · React 19 · Vite 8 · TypeScript.

## Container image

Release images are published to `ghcr.io/marcosaalbanojunior/prontuario-unico` by the maintainer's CI, built from the `v*` tag of the same version in this repository. CI runs outside this repository; see [ADR 0003](docs/adr/0003-ci-cd-outside-public-repo.md).

If you would rather not run a prebuilt image, build it from source. The native build takes several minutes and needs about 8 GB of RAM:

```bash
git checkout v0.1.0            # the release you want
cd deploy && cp .env.example .env   # then fill in .env
docker compose build
docker compose up -d
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). To report a vulnerability, see [SECURITY.md](SECURITY.md).

## License

[Apache License 2.0](LICENSE).
