# 0001. Ship the backend as a GraalVM native image

Date: 2026-10-12 · Status: accepted

## Context

prontuario-unico runs on the owner's own server, often a small VPS that also hosts other services. It is used by one person, so the app sits idle most of the time. A JVM process with Spring Boot typically needs 200–300 MB of resident memory and several seconds to start.

## Decision

Compile the Spring Boot backend to a GraalVM native image (GraalVM CE for JDK 25) and ship it in a distroless image with only the binary. Daily development and most tests run on the JVM; the native build and `native:test` run in CI.

## Consequences

- Target idle RSS around 80 MB and sub-second startup, to be measured and published in the README.
- Every library must work without unregistered runtime reflection, proxies or classpath scanning. This rules out Hibernate (ADR 0002), Spring AI and Telegram bot SDKs; JSON records need explicit reflection hints.
- Native builds take minutes and several GB of RAM, so the server never builds: it pulls a ready image. Self-hosters who want to build from source need a machine that can afford it.
- Anything that touches reflection, resources or serialization must be covered by `native:test`.
