# Security Requirements

## General
- Never hardcode secrets, passwords, or API keys — use environment variables or Vault
- All endpoints require authentication unless explicitly marked `@Public`
- Use HTTPS everywhere — no plain HTTP in production

## Authentication & Authorization
- Use JWT tokens (RS256 signing)
- Token expiry: 15 minutes (access), 7 days (refresh)
- Implement RBAC with roles: ADMIN, USER, READ_ONLY
- Validate all inputs — use `@Valid` on request bodies in Spring

## Data Protection
- Hash passwords with BCrypt (strength 12)
- Mask PII in logs (no emails, SSNs, card numbers in log output)
- Encrypt sensitive DB columns at rest

## Dependencies
- Run `npm audit` and `./gradlew dependencyCheckAnalyze` in CI
- No dependencies with known critical CVEs
- Update dependencies monthly