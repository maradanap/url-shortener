# URL Shortener

A production-style, locally runnable URL Shortener built with Java and
Spring Boot. The project demonstrates requirement decomposition,
greenfield and brownfield engineering, ambiguity resolution, controlled
AI-assisted development, persistent storage, caching, safe schema
management and automated validation.

## Status

- Build: Passing
- Automated tests: 56 passed, 0 failures, 0 errors
- Manual API scenarios: 19 passed
- Database: File-backed H2
- Cache: Caffeine
- Schema management: Flyway
- Java: 21

## Review Guide

For a structured review:

1. [Requirements](docs/REQUIREMENTS.md)
2. [Architecture](docs/ARCHITECTURE.md)
3. [Engineering scenarios](docs/SCENARIOS.md)
4. [Testing evidence](docs/TESTING.md)
5. [Risk register](docs/RISK-REGISTER.md)
6. [AI usage and engineer oversight](docs/AI-USAGE-LOG.md)
7. [OpenAPI definition](docs/openapi.yaml)
8. [Final engineering summary](docs/FINAL-SUMMARY.md)

## Assignment Traceability

| Requirement | Evidence |
|---|---|
| Working prototype | Application source and quick-start instructions |
| Architecture overview | `docs/ARCHITECTURE.md` |
| Greenfield scenario | `docs/SCENARIOS.md` |
| Brownfield scenario | `docs/SCENARIOS.md` |
| Ambiguous scenario | `docs/SCENARIOS.md` |
| API definition | `docs/openapi.yaml` |
| Database schema | Flyway migration scripts |
| Automated validation | 56 passing tests |
| Manual validation | 19 Postman scenarios |
| Risks and trade-offs | `docs/RISK-REGISTER.md` |
| AI-assisted execution | `docs/AI-USAGE-LOG.md` |
| Engineer sign-off | `docs/FINAL-SUMMARY.md` |

## Quick Start on Windows

### Prerequisites

- JDK matching the version in `pom.xml`
- Git
- No separate Maven installation is required
- No PostgreSQL or Redis installation is required

### Build and test

```bat
git clone https://github.com/maradanap/url-shortener.git
cd url-shortener
mvnw.cmd clean verify

curl http://localhost:8080/actuator/health
```

## Bash quick start

```markdown
## Quick Start with Bash

```bash
git clone https://github.com/maradanap/url-shortener.git
cd url-shortener
chmod +x mvnw
./mvnw clean verify
./mvnw spring-boot:run