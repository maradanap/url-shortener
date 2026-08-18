# Engineering Scenarios

This document demonstrates requirement decomposition, implementation, validation, and engineering judgment for greenfield, brownfield, and ambiguous scenarios.

## Greenfield Scenario: Initial URL Shortener

### Problem

Build a locally runnable URL Shortener that creates short URLs,
redirects callers, persists records and returns controlled errors.

### Constraints

- Java and Bash were available locally.
- PostgreSQL and Redis were unavailable.
- The prototype had to remain runnable without external infrastructure.
- The implementation had to demonstrate production-style engineering decisions.

### Decomposition

1. Normalize functional and non-functional requirements.
2. Define REST API contracts.
3. Define the persistence schema.
4. Configure Flyway-controlled migrations.
5. Implement domain entities and repositories.
6. Implement URL validation and short-code generation.
7. Implement creation and redirect services.
8. Add controlled exception handling.
9. Add health monitoring.
10. Validate through unit, integration and manual tests.

### Implementation

- Spring Boot modular monolith
- Spring MVC REST controllers
- Application service layer
- JPA repositories
- File-backed H2 database
- Flyway schema migrations
- Base62 short-code generation using SecureRandom
- Database uniqueness constraint
- Consistent API errors
- Actuator health endpoint

### Validation

- Unit tests for validation and domain behavior
- Service tests for application workflows
- MockMvc integration tests
- Postman end-to-end testing
- Persistence verification after restart
- Maven quality gate

### Result

The greenfield implementation produced a runnable URL Shortener
with controlled API behavior, persistent records and automated validation.

## Brownfield Scenario: Correcting the Cache Boundary

### Existing System

The application already created, resolved and persisted short URLs.
Caffeine was configured and the lookup method used `@Cacheable`.

### Problem Identified

The cached lookup method was invoked by another method within the same
service class. Spring's proxy-based caching does not intercept local
self-invocation, so the annotation could be bypassed.

### Impact Analysis

| Area | Impact |
|---|---|
| Redirect service | Must delegate lookup through a separate Spring bean |
| Transactions | Read-only lookup transaction must remain explicit |
| Cache | Cache key and cache name must remain unchanged |
| Disable operation | Existing cache eviction must continue working |
| Tests | Cache population and eviction require integration validation |
| API | No public API change |

### Change

A dedicated `ShortUrlLookupService` was introduced. The redirect service
now calls that external Spring-managed bean, allowing the caching proxy to
intercept the lookup.

### Risks

- Stale cached values after disabling a URL
- Regression in redirect behavior
- Incorrect transaction boundary
- Local cache behavior differing across multiple instances

### Controls

- Existing cache name and key were preserved.
- Disable operations continue to evict cached entries.
- A cache integration test verifies repeated lookup behavior.
- A regression test verifies that disabled URLs return `410 Gone`.
- The complete 56-test suite was executed after the change.

### Result

The cache now operates through a valid Spring proxy boundary without
changing the public API. All 56 automated tests passed.

## Ambiguous Scenario: Add Analytics

### Original Requirement

"Add analytics."

### Ambiguities

- What counts as a click?
- Do missing URLs count?
- Do expired or disabled URLs count?
- Do repeat visitors count multiple times?
- Is analytics immediately consistent?
- Should raw IP addresses be stored?
- What time zone should be used?
- What is the retention period?

### Decisions

| Question | Decision | Rationale |
|---|---|---|
| Successful redirect | Count | Represents completed business behavior |
| Unknown code | Do not count | No redirect occurred |
| Expired URL | Do not count | No successful redirect occurred |
| Disabled URL | Do not count | No successful redirect occurred |
| Repeated redirects | Count each event | Analytics represent redirect events |
| Consistency | Synchronous | Simple and immediately visible for the prototype |
| Time zone | UTC | Avoids local time-zone ambiguity |
| Raw IP address | Do not store | Reduces privacy exposure |
| IP handling | SHA-256 hash | Prototype-level data minimization |
| Retention | Indefinite locally | Requirement was unspecified; production policy required |

### Implementation

A click event is persisted synchronously immediately before a successful
redirect response is returned. Analytics expose total clicks and the most
recent access time.

### Validation

- Successful redirects increment analytics.
- Multiple redirects increase the count.
- Missing, expired and disabled URLs do not create click events.
- Analytics for URLs without clicks return zero.
- Manual Postman tests confirmed end-to-end behavior.

### Limitations

- Synchronous writes increase redirect latency.
- Unsalted IP hashing is not full anonymization.
- Retention and deletion policies are not implemented.
- Production should consider a durable asynchronous event pipeline.

