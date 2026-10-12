# AI Spring Lab

## Project

This project is a Spring Boot REST API for order management.

## Architecture

Use the following dependency flow:

Controller -> Service -> Repository

Controllers must not directly access repositories.

## Java

- Java 21
- Constructor injection
- Records for immutable DTOs
- BigDecimal for monetary values
- Never use double or float for monetary calculations

## Spring

- Business logic belongs in services.
- Transactions belong at service boundaries.
- Validate REST request bodies.
- Do not expose JPA entities directly through the API.

## Testing

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- Testcontainers for database integration tests

## Verification

Before considering a task complete:

1. Compile the project.
2. Run relevant tests.
3. Run the complete test suite.
4. Review the Git diff.
5. Do not modify unrelated files.

## Important

Do not invent business rules.

If a requirement is ambiguous, ask for clarification.
