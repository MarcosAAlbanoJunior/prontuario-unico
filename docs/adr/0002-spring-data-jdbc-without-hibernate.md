# 0002. Use Spring Data JDBC and JdbcClient instead of JPA/Hibernate

Date: 2026-10-12 · Status: accepted

## Context

The data model is small and centered on `event`, with a handful of related tables. The important reads are aggregations (goal progress, symptom counts per 30 days, timeline across tables) that are clearer in SQL than in an ORM query language. The backend is compiled to a native image (ADR 0001), where Hibernate adds reflection configuration, bytecode enhancement and memory.

## Decision

Use Spring Data JDBC for simple aggregate persistence and `JdbcClient` for explicit SQL queries. The schema is owned by Flyway migrations, never generated from entities. No JPA, no Hibernate.

## Consequences

- SQL stays explicit and readable; aggregations and views are written by hand and tested against PostgreSQL with Testcontainers.
- No lazy loading, dirty checking or entity caching: persistence rows are plain records mapped to the domain at the adapter boundary.
- Lower memory and fewer native-image surprises.
- Some boilerplate that an ORM would generate (mappers, joins) is written by hand.
