# Testing Strategy

This document describes the automated and manual validation performed to establish the correctness, reliability, security, persistence, caching behavior, and safe-change characteristics of the URL Shortener.

## Testing Objectives

The testing process verifies that:

- Valid HTTP and HTTPS URLs can be shortened.
- Short codes can be generated automatically.
- Valid custom aliases can be used.
- Duplicate custom aliases cannot be created.
- Short URLs redirect using HTTP `302 Found`.
- Successful redirects increment analytics.
- Missing, expired, and disabled URLs do not redirect.
- Invalid requests return consistent and controlled errors.
- URL metadata remains available after a soft delete.
- Caffeine caches repeated short-code lookups.
- Cache entries are evicted when URLs are disabled.
- Flyway creates and validates the database schema.
- H2 records persist after an application restart.
- Concurrent requests cannot create duplicate aliases.
- Internal database errors and stack traces are not exposed to API clients.

## Test Environment

| Item | Value |
|---|---|
| Test date | August 18, 2026 |
| Application | URL Shortener |
| Runtime | Local Spring Boot application |
| Java | Project-configured JDK 21 |
| Operating system | Windows |
| Production-profile database | File-backed H2 |
| Automated-test database | In-memory H2 |
| Schema management | Flyway |
| Cache | Caffeine |
| API testing tool | Postman |
| Automated testing | JUnit Jupiter, Mockito, AssertJ, Spring Boot Test and MockMvc |
| Build tool | Maven Wrapper |
| Base URL | `http://localhost:8080` |

No production data, external database, Redis server, or external infrastructure was required to execute the test suite.

## Testing Levels

The project uses multiple testing levels:

1. Domain unit tests
2. Validation and utility tests
3. Application service tests
4. Cache integration tests
5. REST API integration tests
6. Database and Flyway integration tests
7. Concurrency tests
8. Manual Postman API tests
9. Application restart and persistence verification

## Unit Tests

Unit tests validate isolated domain behavior and application logic without starting the complete Spring application.

### URL validation

`UrlValidatorTest` verifies:

- Valid HTTPS URLs are accepted.
- Valid HTTP URLs are accepted.
- Unsupported schemes are rejected.
- URLs without a scheme are rejected.
- URLs without a valid host are rejected.
- `localhost` is rejected.
- Loopback addresses are rejected.
- Syntactically invalid URLs are rejected.

### Short-code generation

`ShortCodeGeneratorTest` verifies:

- Generated codes use the configured length.
- Generated codes contain only Base62 characters.
- Alternative configured lengths are supported.

The test does not assume that random collisions are impossible. Collision handling is tested separately in the application service.

### Domain behavior

`ShortUrlTest` verifies:

- Active links without expiration can redirect.
- Active links with future expiration can redirect.
- Expired links cannot redirect.
- A link expiring at the current instant is considered expired.
- Disabled links cannot redirect.
- Domain fields are initialized correctly.

### URL-shortening service

`UrlShorteningServiceTest` verifies:

- URL creation with a custom alias.
- URL creation with a generated code.
- Duplicate custom-alias rejection.
- Database uniqueness violations are converted to controlled conflicts.
- Past expiration timestamps are rejected.
- Generated-code collisions cause a retry.
- Collision retry exhaustion produces a controlled failure.
- Existing metadata can be retrieved.
- Missing metadata returns a not-found exception.
- Existing URLs can be disabled.
- Disabling an unknown URL returns a not-found exception.

### Lookup service

`ShortUrlLookupServiceTest` verifies:

- Existing short URLs are returned.
- Missing short URLs produce a controlled not-found exception.

### Redirect service

`RedirectServiceTest` verifies:

- Successful redirect lookup returns the correct destination.
- A successful redirect records a click event.
- Click timestamps use the configured UTC clock.
- IP addresses are hashed rather than stored directly.
- Expired URLs do not record clicks.
- Disabled URLs do not record clicks.
- Missing URLs do not record clicks.
- Optional referrer, user-agent and IP information can be absent.

### Analytics service

`AnalyticsServiceTest` verifies:

- Total click count is returned.
- Last-access time is returned.
- URLs without clicks return a zero count and no last-access time.
- Unknown short codes return a controlled not-found response.

## Cache Integration Tests

`ShortUrlCacheIntegrationTest` runs with the Spring application context and the configured Caffeine cache.

It verifies:

- The first short-code lookup retrieves the record from H2.
- The lookup result is placed in the `shortUrls` cache.
- A repeated lookup can be served from Caffeine.
- Disabling a URL evicts its cached entry.
- A disabled URL is not served using stale cached state.

Caching is kept behind a dedicated `ShortUrlLookupService` so Spring can intercept the external `@Cacheable` method call through its proxy.

## REST API Integration Tests

`UrlShortenerIntegrationTest` uses:

- `@SpringBootTest`
- `MockMvc`
- In-memory H2
- Flyway migrations
- Spring Data JPA
- Bean Validation
- Global exception handling
- Caffeine

The integration suite verifies:

- Creation using a custom alias returns `201 Created`.
- Creation without an alias generates a valid short code.
- The creation response contains a `Location` header.
- Metadata retrieval returns `200 OK`.
- Redirect returns `302 Found`.
- Redirect returns the correct destination in the `Location` header.
- Successful redirects increment analytics.
- Multiple redirects increment the click count multiple times.
- Duplicate aliases return `409 Conflict`.
- Invalid URL schemes return `400 Bad Request`.
- Missing URLs return field-validation errors.
- Invalid aliases return `400 Bad Request`.
- Past expiration timestamps return `400 Bad Request`.
- Unknown redirect codes return `404 Not Found`.
- Unknown metadata codes return `404 Not Found`.
- Disabling a URL returns `204 No Content`.
- Disabled URLs return `410 Gone`.
- Disabled-link metadata remains available with `active=false`.

## Database and Migration Validation

Automated integration tests use:

```text
jdbc:h2:mem:urlshortener-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
```

Flyway executes the production migration scripts against this database.

Hibernate uses:

```yaml
ddl-auto: validate
```

This verifies that:

- Flyway can create the schema from a clean database.
- JPA entities match the migrated schema.
- Required tables, columns, constraints and indexes are valid.
- Tests do not rely on Hibernate automatically creating tables.
- Automated tests do not modify the local file-backed database.

## Concurrency Testing

`DuplicateAliasConcurrencyTest` starts multiple threads that attempt to create the same custom alias simultaneously.

Acceptance criteria:

- Exactly one request succeeds.
- All remaining requests receive controlled conflicts.
- Exactly one database record exists for the shared alias.
- The database unique constraint remains the final concurrency safeguard.
- Database constraint details are not exposed to the caller.

The concurrency test passed.

## Automated Test Execution

The complete automated test suite was executed using the Maven Wrapper:

```bat
mvnw.cmd clean verify
```

## Automated Test Results

| Result | Value |
|---|---:|
| Tests executed | 56 |
| Tests passed | 56 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Pass rate | 100% |
| Build result | SUCCESS |

Maven reported:

```text
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The 100% result above represents the test pass rate. It does not mean that the project has 100% source-code coverage.

## Manual API Testing

Manual API tests were performed using Postman against:

```text
http://localhost:8080
```

Postman automatic redirect following was disabled while validating redirect responses. This allowed direct verification of the application's `302 Found` response and `Location` header.

## Manual API Test Results

| ID | Scenario | Request | Expected result | Actual result | Status |
|---|---|---|---|---|---|
| MT-01 | Application health | `GET /actuator/health` | `200 OK` with status `UP` | Returned `200 OK` and `UP` | PASS |
| MT-02 | Create custom alias | `POST /api/v1/urls` | `201 Created` with metadata | Returned expected response | PASS |
| MT-03 | Generate short code | `POST /api/v1/urls` without alias | `201 Created` with generated code | Returned generated code | PASS |
| MT-04 | Retrieve metadata | `GET /api/v1/urls/{shortCode}` | `200 OK` with metadata | Returned expected metadata | PASS |
| MT-05 | Redirect short URL | `GET /{shortCode}` | `302 Found` with correct `Location` | Returned expected redirect | PASS |
| MT-06 | Record analytics | Redirect followed by analytics request | Click count increases | Click count increased | PASS |
| MT-07 | Retrieve analytics | `GET /api/v1/urls/{shortCode}/analytics` | `200 OK` with analytics | Returned expected analytics | PASS |
| MT-08 | Duplicate custom alias | Create an existing alias | `409 Conflict` | Returned `409 Conflict` | PASS |
| MT-09 | Invalid URL scheme | Create a `javascript:` URL | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-10 | Missing URL | Create request without `url` | `400` with field error | Returned controlled field error | PASS |
| MT-11 | Invalid alias length | Alias shorter than four characters | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-12 | Invalid alias characters | Alias with unsupported characters | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-13 | Past expiration | Create with past expiration | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-14 | Unknown redirect code | `GET /not-found-999` | `404 Not Found` | Returned `404 Not Found` | PASS |
| MT-15 | Unknown metadata code | `GET /api/v1/urls/not-found-999` | `404 Not Found` | Returned `404 Not Found` | PASS |
| MT-16 | Disable URL | `DELETE /api/v1/urls/{shortCode}` | `204 No Content` | Returned `204 No Content` | PASS |
| MT-17 | Redirect disabled URL | Redirect after disabling | `410 Gone` | Returned `410 Gone` | PASS |
| MT-18 | Retrieve disabled metadata | Get metadata after disabling | `200` with `active=false` | Returned expected metadata | PASS |
| MT-19 | Persistence after restart | Restart and retrieve existing URL | Existing record remains available | Record remained available | PASS |

## Manual Test Summary

| Result | Count |
|---|---:|
| Manual tests executed | 19 |
| Passed | 19 |
| Failed | 0 |
| Blocked | 0 |
| Pass rate | 100% |

## Combined Validation Summary

| Validation type | Executions | Passed | Failed |
|---|---:|---:|---:|
| Automated tests | 56 | 56 | 0 |
| Manual API scenarios | 19 | 19 | 0 |
| Total recorded executions | 75 | 75 | 0 |

Some manual and automated tests validate the same requirements at different levels. The combined total represents recorded test executions, not 75 unique system requirements.

## Key Behaviors Verified

### Redirect behavior

The application returns:

```text
HTTP/1.1 302 Found
Location: https://example.com/destination
```

Postman redirect following was disabled so this behavior could be directly inspected.

### Analytics behavior

Only successful redirects increment business analytics.

The following do not count as successful clicks:

- Unknown short codes
- Expired URLs
- Disabled URLs
- Request-validation failures

### Soft-delete behavior

After disabling a URL:

- Redirect returns `410 Gone`.
- Metadata remains available.
- Metadata contains `active=false`.
- Historical analytics remain available.
- The Caffeine cache entry is evicted.

### Persistence behavior

The application was stopped and restarted. Previously created records remained available, verifying file-backed H2 persistence.

### Concurrency behavior

Multiple simultaneous requests attempted to claim the same custom alias. Exactly one creation succeeded and the database contained only one record for the alias.

## Negative and Security Testing

The test suite verifies:

- Unsupported URL schemes are rejected.
- URLs without valid hosts are rejected.
- Local destination hosts are rejected.
- Missing required fields are rejected.
- Invalid aliases are rejected.
- Past expiration timestamps are rejected.
- Unknown resources return controlled errors.
- Expired and disabled resources do not redirect.
- Failed redirects do not create analytics events.
- Raw IP addresses are not stored directly.
- Internal exception details and stack traces are not returned to clients.
- Database uniqueness constraints prevent duplicate aliases.

## Regression Testing

The complete automated suite was executed after adding:

- Custom aliases
- Expiration
- Analytics
- Soft delete
- Caffeine caching
- Cache eviction
- Controlled exception handling
- Concurrency protection

All 56 automated tests passed, confirming that these changes did not break the validated creation, metadata, redirect and analytics behavior.

## Quality Gate

The following command is the required local quality gate:

```bat
mvnw.cmd clean verify
```

A change is considered acceptable only when:

- Maven reports `BUILD SUCCESS`.
- No tests fail.
- No test errors occur.
- Flyway migrations succeed.
- Hibernate schema validation succeeds.
- Integration tests use the test profile.
- The production file-backed database is not modified by automated tests.

## Exit Criteria

The tested prototype satisfies its validation exit criteria:

- [x] All 56 automated tests pass.
- [x] All 19 manual API tests pass.
- [x] Maven reports `BUILD SUCCESS`.
- [x] Flyway applies the schema successfully.
- [x] Hibernate validates the migrated schema.
- [x] Health endpoint reports `UP`.
- [x] Redirect behavior returns the correct status and destination.
- [x] Analytics increments after successful redirects.
- [x] Duplicate aliases are rejected.
- [x] Expired and disabled URLs cannot redirect.
- [x] Cache population and eviction are verified.
- [x] Concurrent alias creation is controlled.
- [x] File-backed H2 data persists after restart.
- [x] Controlled API errors do not expose stack traces.

## Known Testing Limitations

The prototype has not been validated for:

- Production-scale traffic volume
- Long-duration soak testing
- Multiple horizontally scaled application instances
- Distributed cache consistency
- Redis integration
- PostgreSQL-specific runtime behavior
- Distributed rate limiting
- Durable asynchronous analytics
- Database failover and high availability
- Network partition recovery
- Multi-region deployment
- Full phishing and malicious-domain detection
- Penetration testing
- Formal performance service-level objectives

These limitations are accepted for the local prototype and are documented as areas for production evolution.

## Final Testing Conclusion

The URL Shortener passed all 56 automated tests and all 19 manual API scenarios with zero failures and zero errors.

Testing established evidence for domain correctness, input validation, persistence, Flyway migrations, caching, cache eviction, REST API behavior, analytics, controlled error handling, soft deletion and duplicate-alias concurrency protection.

The successful test results support demonstration readiness for the defined prototype scope. They do not remove the need for additional performance, security, distributed-system and operational validation before a production deployment.