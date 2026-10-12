# 0003. Run CI/CD outside the public repository

Date: 2026-10-12 · Status: accepted

## Context

prontuario-unico is open source and meant to be self-hosted. The maintainer wants to keep CI/CD (test runners, native builds, image publishing, credentials) in a private repository instead of GitHub Actions in this one. People who install the app still need a trustworthy image, and contributors still need to know which checks a change must pass.

## Decision

- This repository contains no GitHub Actions workflows.
- `scripts/verify.sh` is the single list of checks a change must pass (backend tests on the JVM, frontend lint, tests and build). Contributors run it locally; the private CI runs the same script, followed by `native:test`, a smoke test of the native image and security scans.
- The private CI builds release images from the `v*` tags of this repository and publishes them to the public GHCR package `ghcr.io/marcosaalbanojunior/prontuario-unico`. The Dockerfile labels the image with this repository as its source.
- `.github/dependabot.yml` stays here, since it only opens update pull requests for this repository.

## Consequences

- Pull requests from contributors get no automatic checks on GitHub. Maintainers run `scripts/verify.sh` (or the private CI) before merging.
- Users cannot see the build logs. To keep images auditable, every image matches a tag in this repository, and the README explains how to build the image from source with `docker compose build`.
- The CI definition can drift from this repository; keeping all checks in `scripts/verify.sh` limits that to the native and release steps.
