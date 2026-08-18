# Requirements

This document defines the functional requirements, non-functional requirements, assumptions, ambiguities, and acceptance criteria for the URL Shortener.

## Functional Requirements

List URL creation, redirection, metadata, analytics, expiration, custom alias, and disable/delete behavior.

## Non-Functional Requirements

Describe performance, reliability, security, maintainability, observability, and scalability expectations.

## Validation Rules

Document accepted URL schemes, maximum lengths, alias format, and expiration requirements.

## Assumptions

Record decisions made where the assignment did not provide enough detail.

## Ambiguities and Decisions

| Ambiguity | Decision | Rationale |
|---|---|---|
| Redirect status | `302 Found` | Avoid permanent browser caching |
| Expired URL response | `410 Gone` | Clearly indicates a previously available resource |
| Duplicate destination URLs | Allow separate codes | Supports separate campaigns and analytics |
| Analytics definition | Count successful redirects | Failed requests should not inflate business metrics |

## Out of Scope

List authentication, distributed deployment, geographic analytics, bot detection, and browser-based UI.

## Acceptance Criteria

Define observable conditions that must be true before the prototype is considered complete.