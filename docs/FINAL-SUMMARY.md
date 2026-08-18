# Final Engineering Summary

## 1. Executive Summary

The project delivers a working, locally runnable URL Shortener implemented with Java and Spring Boot.

The application supports:

- Generated short codes
- Custom aliases
- Optional URL expiration
- HTTP redirects
- Metadata retrieval
- Redirect analytics
- Soft deletion
- Persistent local storage
- Caffeine caching
- Flyway-controlled database migrations
- Controlled API errors
- Health and standard metrics
- Automated and manual validation

The project was implemented as a production-style prototype using H2 and Caffeine because PostgreSQL and Redis were unavailable in the local environment.

The application is complete for the defined prototype scope. It is not represented as production-ready without the additional distributed-system, security, availability, privacy, and operational controls documented in this repository.

## 2. Problem Statement

The application must allow a caller to submit a valid HTTP or HTTPS URL and receive a shorter URL.

When the short URL is requested, the application must:

1. Resolve the short code.
2. Confirm that the URL exists.
3. Confirm that the URL is active.
4. Confirm that the URL is not expired.
5. Record a successful redirect event.
6. Return `302 Found` with the original destination.

The system must also provide metadata, analytics, controlled error handling, persistent storage, and safe behavior under concurrent alias creation.

## 3. Local Constraints

The local environment provided:

- Java
- Windows Command Prompt
- Bash
- Git
- GitHub

The local environment did not provide:

- PostgreSQL
- Redis
- Kafka
- Container infrastructure
- Production monitoring infrastructure

The implementation therefore uses:

- File-backed H2 instead of PostgreSQL
- Caffeine instead of Redis
- Synchronous click persistence instead of Kafka
- Maven Wrapper instead of requiring a Maven installation

These substitutions are isolated and documented so that the production evolution remains clear.

## 4. Implemented Scope

### URL creation

The application supports:

```text
POST /api/v1/urls
```

A request may contain:

- Required original URL
- Optional custom alias
- Optional expiration timestamp

The service returns:

- `201 Created`
- `Location` header
- Short code
- Short URL
- Original URL
- Creation time
- Optional expiration
- Active state

### Redirect

The application supports:

```text
GET /{shortCode}
```

Behavior:

- Active URL: `302 Found`
- Missing URL: `404 Not Found`
- Expired URL: `410 Gone`
- Disabled URL: `410 Gone`

### Metadata

The application supports:

```text
GET /api/v1/urls/{shortCode}
```

Metadata remains available after a soft delete.

### Analytics

The application supports:

```text
GET /api/v1/urls/{shortCode}/analytics
```

Analytics return:

- Short code
- Total successful redirects
- Most recent access time

### Disable

The application supports:

```text
DELETE /api/v1/urls/{shortCode}
```

The operation performs a soft delete and returns:

```text
204 No Content
```

## 5. Engineering Approach

The work was executed using the following engineering sequence:

1. Normalize functional and non-functional requirements.
2. Identify ambiguities and make explicit decisions.
3. Define scope and non-goals.
4. Select locally runnable technologies.
5. Define REST API and database contracts.
6. Create a Flyway-controlled schema.
7. Implement domain entities and repositories.
8. Implement application services and validation.
9. Implement controllers and controlled error handling.
10. Add persistent H2 storage.
11. Add Caffeine caching.
12. Correct the Spring caching proxy boundary.
13. Add analytics and soft deletion.
14. Add unit, service, integration, cache, and concurrency tests.
15. Execute manual Postman validation.
16. Verify persistence after application restart.
17. Document risks, trade-offs, limitations, and production evolution.
18. Review AI-assisted outputs and retain engineer ownership.

## 6. Delivered Artifacts

### Application artifacts

- Spring Boot application
- REST controllers
- Request and response models
- Application services
- Domain entities
- Domain exceptions
- JPA repositories
- URL validator
- SecureRandom Base62 code generator
- Caffeine cache configuration
- UTC clock configuration
- Global API exception handler
- Actuator health and standard metrics

### Database artifacts

- H2 local datasource configuration
- H2 automated-test datasource configuration
- Flyway migration
- Short URL table
- Click-event table
- Unique constraint
- Foreign key
- Analytics indexes
- Optimistic-lock version column

### API artifacts

- REST endpoints
- Controlled API error model
- OpenAPI definition in `docs/openapi.yaml`
- Postman/cURL validation scenarios

### Testing artifacts

- Domain unit tests
- URL-validation tests
- Code-generation tests
- Application-service tests
- Redirect tests
- Analytics tests
- Cache integration tests
- REST API integration tests
- Flyway and schema validation
- Duplicate-alias concurrency test
- Manual Postman results
- Maven Wrapper quality gate

### Documentation artifacts

- Requirements
- Architecture
- Engineering scenarios
- AI usage log
- Risk register
- Testing strategy and results
- OpenAPI contract
- Final engineering summary
- Repository README and setup instructions

## 7. Architecture Summary

The system is a layered modular monolith.

```text
API client
    ↓
REST controllers
    ↓
Application services
    ↓
Domain validation and decisions
    ↓
Cached lookup / repositories
    ↓
Caffeine and H2
```

Key architecture characteristics:

- Controllers remain thin.
- Business decisions remain in services and domain classes.
- Persistence is accessed through repository interfaces.
- Flyway owns database schema creation.
- Hibernate validates rather than creates the schema.
- Caffeine accelerates repeated short-code lookups.
- H2 remains the source of truth.
- Cache eviction occurs after disabling a URL.
- Expiration and active-state checks occur after cache retrieval.
- UTC is used for creation, expiration, and click timestamps.

## 8. Data Model Summary

### Short URL

The short URL record contains:

- Database identifier
- Unique short code
- Original URL
- Creation time
- Optional expiration
- Active state
- Optimistic-lock version

### Click event

The click event contains:

- Database identifier
- Short URL reference
- Click timestamp
- Optional referrer
- Optional user agent
- Optional IP hash

The database enforces alias uniqueness and referential integrity.

## 9. Key Engineering Decisions

| Decision | Rationale |
|---|---|
| Modular monolith | Appropriate scope with clear boundaries and low operational complexity |
| H2 file database | Persistent storage without local database installation |
| Flyway | Controlled and reviewable schema evolution |
| Hibernate validation | Prevent silent schema modification |
| Caffeine | Local lookup acceleration without Redis |
| Dedicated lookup service | Ensure Spring caching proxy interception |
| SecureRandom Base62 | Non-sequential, URL-safe codes |
| Database unique constraint | Authoritative concurrency protection |
| UTC `Instant` | Unambiguous time handling |
| Injectable `Clock` | Deterministic expiration testing |
| `302 Found` | Avoid permanent redirect caching |
| `410 Gone` | Clearly represent expired or disabled resources |
| Soft delete | Preserve metadata and analytics |
| Synchronous analytics | Immediate consistency and simpler prototype behavior |
| Concrete service classes | Avoid unjustified one-to-one service interfaces |
| OpenAPI | Reviewable and portable API contract |

## 10. Greenfield Scenario Summary

The greenfield scenario created the initial URL Shortener from normalized requirements.

The work included:

- API contract definition
- Database design
- URL validation
- Code generation
- Creation and redirect behavior
- Persistent storage
- Controlled error handling
- Automated and manual validation

The result was a runnable end-to-end service with persistent data and defined API behavior.

Detailed evidence is available in:

```text
docs/SCENARIOS.md
```

## 11. Brownfield Scenario Summary

The brownfield scenario addressed a caching correctness issue in an existing working application.

The initial cached method was invoked internally within the same class. Spring's proxy-based caching does not intercept local self-invocation.

The change:

- Introduced `ShortUrlLookupService`
- Moved `@Cacheable` lookup behavior into that service
- Updated redirect behavior to use the external service
- Preserved the API contract
- Preserved transaction semantics
- Preserved cache eviction
- Added cache population and eviction integration tests
- Executed the complete regression suite

All 56 automated tests passed after the change.

## 12. Ambiguous Scenario Summary

The requirement “Add analytics” did not define what should count as a click.

The implementation made these decisions:

- Count successful redirects.
- Do not count missing URLs.
- Do not count expired URLs.
- Do not count disabled URLs.
- Count repeated redirects as separate events.
- Use UTC timestamps.
- Make analytics immediately visible.
- Do not store raw IP addresses.
- Retain analytics indefinitely in the prototype because retention was unspecified.

The decisions, rationale, limitations, and validation are documented in:

```text
docs/SCENARIOS.md
```

## 13. AI-Assisted Engineering

AI was used to accelerate:

- Requirement decomposition
- Architecture exploration
- Technology selection
- Code suggestions
- Test generation
- Failure analysis
- Cache review
- Documentation structure
- Risk identification

AI outputs were not accepted automatically.

Examples of engineer oversight include:

- Editing the H2 connection configuration after runtime evidence showed incompatible options
- Rejecting unnecessary one-to-one service interfaces
- Accepting and validating the dedicated cache lookup service
- Reviewing generated test cases against the implemented API
- Running all automated and manual validations
- Correcting documentation that could have overstated unimplemented rate limiting
- Separating implemented capabilities from production-evolution recommendations

Detailed records are available in:

```text
docs/AI-USAGE-LOG.md
```

## 14. Validation Summary

### Automated validation

The Maven verification executed:

```text
56 automated tests
56 passed
0 failures
0 errors
0 skipped
BUILD SUCCESS
```

The automated suite covers:

- URL validation
- Short-code generation
- Domain expiration behavior
- Creation services
- Redirect services
- Analytics services
- Metadata retrieval
- Soft deletion
- Controlled errors
- Cache population
- Cache eviction
- REST API integration
- Flyway migration
- Hibernate schema validation
- Duplicate-alias concurrency

### Manual validation

The Postman validation executed:

```text
19 manual API scenarios
19 passed
0 failed
```

Manual scenarios included:

- Health
- Custom-alias creation
- Generated-code creation
- Metadata
- Redirect
- Analytics
- Duplicate aliases
- Invalid schemes
- Missing fields
- Invalid aliases
- Past expiration
- Missing codes
- Soft deletion
- Disabled redirects
- Persistence after restart

Detailed results are available in:

```text
docs/TESTING.md
```

## 15. Requirement Traceability

| Requirement | Evidence |
|---|---|
| Working end-to-end prototype | Application code and README setup instructions |
| Architecture overview | `docs/ARCHITECTURE.md` |
| Component responsibilities | Architecture component table |
| Control flows | Architecture flow and sequence diagrams |
| Greenfield scenario | `docs/SCENARIOS.md` |
| Brownfield scenario | `docs/SCENARIOS.md` |
| Ambiguous scenario | `docs/SCENARIOS.md` |
| API definition | `docs/openapi.yaml` |
| Database schema | Flyway migration |
| Production-quality code | `src/main/java` |
| Automated tests | `src/test/java` and 56 passing tests |
| Manual validation | Postman results in `docs/TESTING.md` |
| Risk controls | `docs/RISK-REGISTER.md` |
| AI-assisted engineering | `docs/AI-USAGE-LOG.md` |
| Setup instructions | `README.md` |
| Testing approach | `docs/TESTING.md` |
| Trade-offs | Architecture, risk register, and final summary |
| Assumptions and limitations | Requirements, architecture, risk register, and final summary |
| Engineer ownership | AI log and engineer sign-off |

## 16. Risks and Controls Summary

Important controls include:

- HTTP/HTTPS restriction
- URL syntax and host validation
- Input-length limits
- Alias-format validation
- SecureRandom Base62 codes
- Collision retries
- Database uniqueness
- Optimistic locking
- UTC time handling
- Expiration validation
- Active-state validation
- Cache eviction
- Controlled exception handling
- Suppressed stack traces
- Flyway schema ownership
- Hibernate schema validation
- Automated regression tests
- Concurrency validation
- Human review of AI-generated suggestions

Important remaining risks include:

- Open-redirect abuse
- No authentication
- No distributed rate limiting
- Local cache inconsistency across multiple instances
- H2 availability limitations
- Synchronous analytics latency
- Undefined analytics retention
- Unsalted IP hashing
- No production load testing
- No production penetration testing

The complete risk assessment is available in:

```text
docs/RISK-REGISTER.md
```

## 17. Assumptions

The implementation uses these assumptions:

- Callers submit valid JSON.
- Destination URLs use HTTP or HTTPS.
- Redirects use `302 Found`.
- Duplicate original URLs may receive separate short codes.
- Custom aliases are case-sensitive.
- Expiration timestamps are supplied as ISO-8601 values.
- UTC is used for all stored timestamps.
- Successful redirect events represent analytics clicks.
- Repeat redirects count separately.
- Disabled URLs retain metadata and analytics.
- The local application runs as a single instance.
- H2 is sufficient for local prototype validation.
- Caffeine is sufficient for local cache validation.
- Authentication is outside the prototype scope.
- Analytics retention is unspecified.

## 18. Trade-Offs

### H2 instead of PostgreSQL

Benefit:

- No external installation
- Fast local execution
- Repeatable integration testing

Cost:

- No production availability
- Different locking and query behavior
- No real PostgreSQL performance evidence

### Caffeine instead of Redis

Benefit:

- No external cache server
- Simple and fast local caching
- Configurable size and expiration

Cost:

- Not shared between instances
- Cache contents disappear after restart
- No distributed invalidation

### Synchronous analytics

Benefit:

- Immediate visibility
- Simple consistency model
- Easy local validation

Cost:

- Redirect latency includes database-write latency
- A database failure can affect redirects
- Production scaling is limited

### Soft delete

Benefit:

- Preserves analytics and traceability
- Metadata remains reviewable

Cost:

- Data grows without retention policies
- Privacy deletion requirements require additional design

### Concrete application services

Benefit:

- Fewer unnecessary abstractions
- Straightforward navigation and testing

Cost:

- Use-case interfaces would need to be introduced if multiple implementations or separate modules become necessary

## 19. Known Limitations

- H2 is a single-process local database without replication, automated backups, failover, or high availability.
- Caffeine is an application-local cache and is not shared across multiple instances.
- Cache contents are lost when the application restarts; H2 remains the source of truth.
- Distributed rate limiting is not implemented.
- Analytics events are written synchronously, increasing redirect latency.
- Authentication, authorization, and per-user URL ownership are not implemented.
- Malicious-domain reputation checking and abuse monitoring are not implemented.
- IP hashes are unsalted and should not be considered fully anonymized.
- Analytics retention and deletion policies are not implemented.
- Production-scale load, soak, failover, and penetration testing were not performed.
- H2 PostgreSQL compatibility mode is not a replacement for validation against PostgreSQL.
- Centralized logging, tracing, dashboards, and alerting are not implemented.
- Multi-instance cache consistency has not been validated.

## 20. Production Evolution

A production implementation should evolve the prototype as follows:

| Prototype | Production evolution |
|---|---|
| File-backed H2 | PostgreSQL with connection pooling, replication, backups, migrations, and failover |
| Local Caffeine cache | Shared Redis cluster with an explicit invalidation and expiration strategy |
| No distributed rate limiter | API gateway or Redis-backed distributed rate limiting |
| Synchronous click insert | Durable Kafka or messaging-based click-event pipeline |
| Single application instance | Horizontally scaled stateless application instances |
| Basic Actuator endpoints | Centralized metrics, logs, traces, dashboards, and alerting |
| Local configuration | Environment-specific configuration and secret management |
| No authentication | Authenticated URL ownership and authorization policies |
| Indefinite analytics retention | Defined retention, deletion, privacy, and compliance policies |
| Basic URL validation | Domain reputation checks, abuse detection, reporting, and blocking controls |
| Local H2 validation | Integration and performance testing against production PostgreSQL |
| Manual local operation | Containerized deployment and automated delivery pipeline |

These changes are intentionally documented rather than partially implemented because the assignment requires a locally runnable prototype without PostgreSQL or Redis.

## 21. Final Assessment

The URL Shortener satisfies the defined local prototype requirements.

The implementation demonstrates:

- Requirement normalization
- Explicit architecture decisions
- Modular code
- Reviewable API and database contracts
- Persistent storage
- Cache integration
- Controlled schema management
- Consistent error handling
- Concurrency protection
- Automated validation
- Manual end-to-end validation
- Risk identification
- Honest trade-off analysis
- Controlled AI assistance
- Engineer ownership

The successful validation results support submission and repository review for the defined scope.

They do not remove the need for additional security, distributed-system, scalability, reliability, privacy, and operational validation before production deployment.

## 22. Engineer Sign-Off

I reviewed the final application code, database migration, API behavior, automated tests, manual validation results, engineering decisions, risks, and documentation.

AI was used to accelerate decomposition, implementation suggestions, testing, review, and documentation. AI outputs were reviewed, modified, or rejected based on runtime evidence, architectural fit, maintainability, and project scope.

I own the final engineering decisions and the assessment of correctness, maintainability, risks, trade-offs, and limitations.

The application is complete and review-ready for the defined local prototype scope. It is not represented as production-ready without the additional controls documented in this repository.