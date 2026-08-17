# Testing Strategy

This document describes the automated and manual validation used to establish correctness, reliability, security, and safe change management.

## Testing Objectives

Define what the test suite must prove.

## Unit Tests

Test isolated validation, code-generation, expiration, and domain behavior.

## Service Tests

Test application workflows using mocked external boundaries where appropriate.

## Integration Tests

Test controllers, services, repositories, Flyway migrations, and H2 together.

## Concurrency Tests

Verify that concurrent requests cannot create duplicate custom aliases.

## Negative and Security Tests

Test invalid URLs, unsupported schemes, excessive input, missing codes, expired links, and controlled error responses.

## Regression Tests

Verify that brownfield enhancements do not break existing creation and redirection behavior.

## Manual Smoke Testing

Describe the Bash or Windows command-line tests used against the running application.

## Quality Gates

The project must pass:

```bat
mvnw.cmd clean verify