# Risk Register

## 1. Purpose

This document identifies technical, security, privacy, operational, scalability, data, and AI-assisted engineering risks associated with the URL Shortener.

Each risk includes implemented controls, validation evidence, and residual limitations.

The objective is not to claim that all risk has been eliminated. The objective is to demonstrate that risks were identified, evaluated, controlled where appropriate, and honestly documented where they remain.

## 2. Risk Rating Definitions

### Impact

| Rating | Meaning |
|---|---|
| Low | Limited effect with straightforward recovery |
| Medium | Degraded behavior or incorrect results requiring intervention |
| High | Security, availability, data-integrity, or major correctness impact |

### Likelihood

| Rating | Meaning |
|---|---|
| Low | Unlikely under normal prototype use |
| Medium | Plausible without additional controls |
| High | Expected to occur under common conditions |

## 3. Risk Register

| ID | Risk | Category | Impact | Likelihood | Implemented control | Validation evidence | Residual limitation |
|---|---|---|---|---|---|---|---|
| R-01 | Malicious redirect destination | Security | High | Medium | HTTP/HTTPS restriction, host validation, maximum URL length, explicit local-host rejection | URL validation unit tests and negative API tests | Domain reputation, phishing detection, DNS rebinding protection, and abuse reporting are not implemented |
| R-02 | Generated short-code collision | Correctness | Medium | Low | SecureRandom Base62 generation, availability check, configurable retry limit, database unique constraint | Generator tests and collision-retry service tests | Retry limit can be exhausted; caller receives a controlled failure |
| R-03 | Concurrent requests claim the same custom alias | Data integrity | High | Medium | Application availability check, database unique constraint, controlled conflict mapping | Multi-threaded duplicate-alias concurrency test | Conflicting callers must select another alias |
| R-04 | Stale cached URL remains available after disabling | Correctness | High | Medium | Cache eviction on disable and active-state validation after lookup | Cache eviction integration test and disabled-redirect regression test | Multi-instance invalidation is not supported by local Caffeine |
| R-05 | Expired URL remains accessible from cache | Correctness | High | Medium | Expiration is checked after cache retrieval using UTC time | Domain expiration tests and redirect service tests | Cache entry may remain stored until eviction, although it cannot redirect |
| R-06 | Cache self-invocation bypasses Spring proxy | Performance and correctness | Medium | Medium | Cached lookup moved to a dedicated Spring-managed service | Cache population integration test | Future refactoring must preserve the external proxy boundary |
| R-07 | Analytics database write increases redirect latency | Performance | Medium | High | Simple synchronous transaction and indexed foreign key | Redirect integration and analytics tests | Production should use durable asynchronous event processing |
| R-08 | Analytics event is lost if redirect transaction fails | Data integrity | Medium | Low | Click persistence and redirect resolution execute within controlled service logic | Redirect service and integration tests | A production event pipeline requires durable delivery and retry semantics |
| R-09 | Raw IP data creates privacy exposure | Privacy | High | Medium | Raw IP address is not stored; SHA-256 hash is stored instead | Redirect service test verifies hashed value | Unsalted IP hashing is not full anonymization and retention policy is undefined |
| R-10 | Referrer or user-agent values are excessively large | Resource and data quality | Medium | Medium | Values are truncated before persistence and database column lengths are constrained | Redirect service tests and schema validation | Stored text is not semantically validated |
| R-11 | Excessive request volume exhausts resources | Availability | High | Medium | Maximum field lengths, controlled validation, database constraints, and bounded cache size reduce per-request resource usage | Validation and integration tests | Request-rate limiting is not implemented; production requires an API gateway or Redis-backed limiter |
| R-12 | H2 database outage or corruption | Availability and data | High | Low | File-backed persistence, Flyway-controlled schema, application health endpoint | Restart persistence check and integration tests | H2 has no replication, failover, automated backup, or recovery orchestration |
| R-13 | Database schema and JPA entities diverge | Correctness | High | Medium | Flyway owns schema creation and Hibernate uses `ddl-auto: validate` | Application startup and integration-test context initialization | Production migrations require staging validation, backup, rollback planning, and deployment controls |
| R-14 | Database files or build output are committed | Security and repository hygiene | Medium | Medium | `.gitignore` excludes `data/`, `target/`, logs, environment files, and IDE files | Git tracked-file review | Previously committed secrets or files would require history remediation |
| R-15 | Predictable identifiers allow enumeration | Security | Medium | Low | SecureRandom Base62 codes rather than sequential database IDs | Generator tests verify permitted alphabet and length | Public short URLs can still be scanned or shared |
| R-16 | Authentication and URL ownership are absent | Security | High | High | Explicitly documented as outside prototype scope | Requirements and architecture review | Any caller can create or disable URLs if they know a short code; production requires authentication and authorization |
| R-17 | Analytics retention grows indefinitely | Privacy and capacity | Medium | High | Limitation is documented | Architecture and final-summary review | Retention, deletion, partitioning, archival, and compliance policies are not implemented |
| R-18 | Internal exception details are exposed | Security | High | Medium | Global exception handler returns controlled API errors and suppresses stack traces | Negative MockMvc and manual Postman tests | Centralized secure production logging is not implemented |
| R-19 | H2 PostgreSQL compatibility differs from PostgreSQL | Portability | Medium | Medium | Flyway SQL and H2 PostgreSQL compatibility mode | Migration and integration tests | Real PostgreSQL behavior, locking, indexing, query plans, and timestamp semantics require separate validation |
| R-20 | Caffeine cache differs across application instances | Scalability | Medium | High in multi-instance deployment | H2 remains the source of truth and cache is treated as temporary | Cache integration tests | Production requires Redis or another shared-cache strategy |
| R-21 | Health endpoint reveals excessive information | Security | Medium | Low | Only selected Actuator endpoints are exposed and detailed health output is disabled | Manual health endpoint validation | Production endpoint access should be network-restricted and authenticated |
| R-22 | H2 console is exposed outside development | Security | High | Low locally | Console is intended only for local development | Configuration and architecture review | Production configuration must disable or exclude the H2 console |
| R-23 | Time-zone differences cause incorrect expiration | Correctness | High | Medium | UTC `Instant`, Hibernate UTC configuration, and injectable UTC `Clock` | Domain boundary and expiration tests | External clients must still submit valid ISO-8601 timestamps |
| R-24 | Soft delete leaves disabled data indefinitely | Privacy and capacity | Medium | Medium | Soft deletion preserves traceability and analytics | Disable and metadata integration tests | Production requires retention, archival, deletion, and compliance policies |
| R-25 | AI-generated implementation contains defects | Engineering process | High | Medium | Human review, explicit acceptance/edit/rejection decisions, automated tests, manual tests, and runtime validation | AI usage log, 56 automated tests, and 19 manual scenarios | The engineer remains responsible for correctness; tests cannot prove absence of all defects |
| R-26 | Documentation overstates implementation | Engineering process | High | Medium | Claim-to-code audit and explicit separation of implemented features from production evolution | Documentation review and repository grep audit | Documentation must be maintained when implementation changes |
| R-27 | Local test success does not represent production load | Performance and operations | High | High | Scope and limitations are explicitly documented | Test report and final summary | Load, soak, failover, resilience, and penetration testing remain required |
| R-28 | Dependency vulnerability | Security | High | Medium | Spring Boot dependency management and reproducible Maven build | Successful Maven build | Automated dependency scanning and vulnerability remediation workflow are not implemented |

## 4. Highest-Priority Production Risks

The highest-priority risks before production deployment are:

1. Missing authentication and authorization
2. Missing distributed rate limiting
3. Malicious-domain and abuse exposure
4. H2 availability and durability limitations
5. Non-distributed cache behavior
6. Synchronous analytics latency
7. Undefined analytics retention and privacy policy
8. Absence of production load and security testing
9. Absence of centralized operational monitoring
10. Differences between H2 compatibility mode and PostgreSQL

## 5. Accepted Prototype Risks

The following risks are deliberately accepted for the local prototype:

- H2 instead of PostgreSQL
- Caffeine instead of Redis
- Synchronous analytics persistence
- Single application instance
- No authentication
- No distributed rate limiter
- No durable message broker
- No geographic or device analytics
- No domain reputation system
- No production retention policy
- No high-availability database
- No centralized logs, traces, dashboards, or alerts
- No production load, soak, failover, or penetration testing

These decisions keep the prototype runnable with Java and Bash and avoid partially implementing production infrastructure that cannot be validated locally.

## 6. Risk Controls Demonstrated

The prototype demonstrates the following risk controls:

- URL scheme and host validation
- Maximum input lengths
- Alias format validation
- SecureRandom Base62 generation
- Generated-code collision retries
- Database unique constraint
- Controlled conflict mapping
- Optimistic locking
- UTC time handling
- Expiration checks after cache lookup
- Cache eviction after disable
- Soft deletion
- Consistent error responses
- Suppressed stack traces
- Flyway-controlled schema
- Hibernate schema validation
- Automated unit tests
- Spring integration tests
- Cache integration tests
- Concurrency tests
- Manual Postman validation
- Persistence verification after restart
- Human review of AI-generated suggestions

## 7. High-Impact Change Review

The following are treated as high-impact changes:

- Database schema changes
- Changes to alias uniqueness
- Changes to redirect eligibility
- Changes to expiration semantics
- Changes to transaction boundaries
- Changes to analytics counting
- Changes to cache keys or eviction behavior
- Changes to URL validation
- Changes to API status codes
- Changes to authentication or authorization
- Changes to data retention
- Changes generated or substantially influenced by AI

A high-impact change requires:

1. Explicit engineer review
2. Impact analysis
3. Updated or new tests
4. Full regression execution
5. Documentation review
6. Confirmation that public API behavior remains compatible or is intentionally versioned

## 8. Risk Review Conclusion

The prototype's major risks have been identified and either controlled, tested, documented, or explicitly accepted.

The application is appropriate for the defined local prototype scope. It must not be represented as production-ready without the additional security, scalability, availability, privacy, and operational controls described in this register.