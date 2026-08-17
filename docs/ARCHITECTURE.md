# Architecture

This document describes the URL Shortener’s components, technology choices, control flow, data model, and key architectural decisions.

## System Overview

Describe the application at a high level.

## Components

List the controllers, services, repositories, database, cache, and monitoring components.

## Request Flow

Explain the URL creation, redirection, and analytics flows.

## Data Model

Describe the `short_urls` and `click_events` tables.

## Technology Decisions

Explain why Spring Boot, H2, Flyway, Caffeine, and Maven were selected.

## Security and Reliability

Describe validation, rate limiting, error handling, concurrency controls, and observability.

## Production Evolution

Explain how H2, Caffeine, and other prototype components could be replaced in production.