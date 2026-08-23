# Docket

Open-source professional network — LinkedIn parity, NYT-simple design, no gimmicks, free.

The complete v1 design is [`SPEC.md`](./SPEC.md); the domain vocabulary is
[`CONTEXT.md`](./CONTEXT.md). Every decision is settled there — significant design changes are
argued in an issue first (SPEC.md §12).

## Run locally

Requires a JDK 25 and Docker (the dev Postgres and the tests' throwaway databases both run in
containers).

```sh
./mvnw spring-boot:run
```

Spring Boot starts the `compose.yaml` Postgres automatically. The app is at
<http://localhost:8080>.

## Tests

```sh
./mvnw test
```

Integration tests run against a real Postgres via Testcontainers; no local database or
configuration is needed. The same suite runs on every pull request.
