# Testing Strategy

This document describes the automated and manual validation used to establish correctness, reliability, security, and safe change management for the URL Shortener.

## Testing Objectives

The testing process verifies that:

- Valid URLs can be shortened.
- Generated short codes and custom aliases work correctly.
- Short URLs redirect using HTTP `302 Found`.
- Successful redirects increment analytics.
- Invalid requests return controlled error responses.
- Duplicate aliases cannot be created.
- Expired and disabled URLs cannot redirect.
- Metadata remains available after a soft delete.
- Database records persist after an application restart.
- Application health can be monitored.

## Test Environment

| Item | Value |
|---|---|
| Test date | August 18, 2026 |
| Application | URL Shortener |
| Runtime | Local Spring Boot application |
| Java version | OpenJDK 21.0.5 |
| Database | File-backed H2 |
| Schema management | Flyway |
| Cache | Caffeine |
| API client | Postman |
| Base URL | `http://localhost:8080` |
| Operating system | Windows |
| Test data | Non-production sample URLs |

## Manual API Test Results

The following tests were executed using Postman against the locally running application.

| ID | Scenario | Request | Expected result | Actual result | Status |
|---|---|---|---|---|---|
| MT-01 | Application health | `GET /actuator/health` | `200 OK` with status `UP` | Returned `200 OK` and `UP` | PASS |
| MT-02 | Create custom alias | `POST /api/v1/urls` | `201 Created` with URL metadata | Returned `201 Created` with expected metadata | PASS |
| MT-03 | Generate short code | `POST /api/v1/urls` without alias | `201 Created` with generated code | Returned `201 Created` with generated code | PASS |
| MT-04 | Retrieve metadata | `GET /api/v1/urls/{shortCode}` | `200 OK` with stored metadata | Returned expected metadata | PASS |
| MT-05 | Redirect short URL | `GET /{shortCode}` | `302 Found` with correct `Location` header | Returned `302` with correct destination | PASS |
| MT-06 | Record analytics | Redirect followed by analytics request | Click count increases | Click count increased after redirect | PASS |
| MT-07 | Retrieve analytics | `GET /api/v1/urls/{shortCode}/analytics` | `200 OK` with count and last-access time | Returned expected analytics | PASS |
| MT-08 | Duplicate custom alias | Create an existing alias | `409 Conflict` | Returned `409 Conflict` | PASS |
| MT-09 | Invalid URL scheme | Create a `javascript:` URL | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-10 | Missing URL | Create request without `url` | `400 Bad Request` with field error | Returned controlled validation error | PASS |
| MT-11 | Invalid alias length | Create using alias shorter than four characters | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-12 | Invalid alias characters | Create alias containing spaces or special characters | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-13 | Past expiration | Create with an expiration in the past | `400 Bad Request` | Returned `400 Bad Request` | PASS |
| MT-14 | Unknown redirect code | `GET /not-found-999` | `404 Not Found` | Returned `404 Not Found` | PASS |
| MT-15 | Unknown metadata code | `GET /api/v1/urls/not-found-999` | `404 Not Found` | Returned `404 Not Found` | PASS |
| MT-16 | Disable URL | `DELETE /api/v1/urls/{shortCode}` | `204 No Content` | Returned `204 No Content` | PASS |
| MT-17 | Redirect disabled URL | Redirect after disabling | `410 Gone` | Returned `410 Gone` | PASS |
| MT-18 | Retrieve disabled metadata | Get metadata after disabling | `200 OK` with `active=false` | Returned metadata with `active=false` | PASS |
| MT-19 | Persistence after restart | Restart application and retrieve existing URL | Existing record remains available | Existing record remained available | PASS |

## Manual Test Summary

| Result | Count |
|---|---:|
| Tests executed | 19 |
| Passed | 19 |
| Failed | 0 |
| Blocked | 0 |
| Pass rate | 100% |

All planned manual API scenarios passed in the local prototype environment.

This result confirms the tested behavior in the local environment. It does not prove production readiness or performance at production scale.

## Important Behavior Verified

### Redirect behavior

Postman automatic redirect following was disabled during redirect testing. This allowed verification of the URL Shortener's actual response:

```text
HTTP status: 302 Found
Location: https://example.com/products/123