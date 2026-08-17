# Risk Register

This document identifies technical, security, operational, privacy, and AI-assisted engineering risks, along with controls and remaining limitations.

## Risk Assessment

| ID | Risk | Impact | Likelihood | Control | Remaining Limitation |
|---|---|---|---|---|---|
| R-01 | Duplicate short codes | High | Low | Database unique constraint and collision retry | Creation may require another retry |
| R-02 | Malicious destination URL | High | Medium | Allow only HTTP and HTTPS URLs | Abuse monitoring is not implemented |
| R-03 | Cache inconsistency | Medium | Medium | Expiration checks and cache eviction | Caffeine is local to one instance |
| R-04 | Analytics write latency | Medium | Medium | Indexed database writes | Writes are synchronous |
| R-05 | Excessive requests | High | Medium | Input limits and rate limiting | Local limiter is not distributed |
| R-06 | Sensitive data exposure | High | Low | Consistent errors and hashed IP addresses | Retention policy is not implemented |
| R-07 | Database failure | High | Low | Health checks and controlled error handling | H2 has no high availability |
| R-08 | AI-generated defect | High | Medium | Human review and automated tests | Engineer remains responsible for correctness |

## Risk Review Process

Describe when risks are reviewed and how high-impact changes require engineer approval.

## Accepted Risks

Document limitations that were deliberately accepted for the prototype.

## Production Controls

Describe additional controls required for a production deployment.