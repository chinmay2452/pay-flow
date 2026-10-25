# PayFlow --- Project Documentation

> **Status:** Living architecture document\
> **Last updated:** 22 October 2026\
> **Implementation status:** `GET /api/v1/health` is confirmed working.
> PostgreSQL persistence, authentication, payments, ledger,
> reconciliation, frontend, and CI/CD remain planned unless marked
> otherwise.

## 1. Project Overview

PayFlow is an educational payment-processing and reconciliation
simulator built with Java and Spring Boot. It simulates account
creation, payment initiation, status transitions, transaction history,
idempotency, provider failures, ledger entries, and reconciliation. It
will not move real money or use a real payment gateway in the initial
version.

### Goals

-   Learn Spring Boot's request lifecycle and layered architecture.
-   Build versioned REST APIs with clear request/response contracts.
-   Persist data in PostgreSQL using Spring Data JPA and Hibernate.
-   Version schema changes with Flyway.
-   Implement payment states, idempotency, a mock provider, ledger, and
    reconciliation.
-   Write unit, API, and integration tests.
-   Introduce GitHub and Jenkins CI early.
-   Add a React + TypeScript frontend after the backend is stable.

### Non-goals

-   Real-money processing or real payment gateway integration.
-   Real card/bank credential handling.
-   Claiming production banking-grade security or compliance.
-   Premature microservices, Kafka, Kubernetes, or distributed
    infrastructure.

### Success criteria

-   Predictable HTTP responses and validation errors.
-   Duplicate requests handled safely with idempotency.
-   Explicit payment-state transitions.
-   Related local database changes commit or roll back together.
-   Ledger records are auditable and reconciliation finds seeded
    mismatches.
-   Tests run locally and in Jenkins.

## 2. Current Status

### Confirmed

-   Spring Boot Maven project generated.
-   Root package: `com.playflow.playflow1`.
-   `HealthController` created.
-   `GET /api/v1/health` returns JSON successfully.

Expected response:

``` json
{
  "status": "UP",
  "application": "PayFlow",
  "message": "PayFlow is running!"
}
```

### Not yet confirmed / planned

-   PostgreSQL connection and persistence.
-   Migrations, authentication, account/payment APIs.
-   Provider simulation, idempotency, ledger, reconciliation.
-   Expanded tests, Jenkins pipeline, React frontend.

An earlier startup attempt failed because no datasource URL was
configured. Verify database connectivity before claiming persistence
works. Do not permanently disable JPA auto-configuration as a substitute
for configuring PostgreSQL.

## 3. Technology Stack

  ------------------------------------------------------------------------------
  Area                    Choice                  Purpose
  ----------------------- ----------------------- ------------------------------
  Language                Java                    Backend

  Framework               Spring Boot 4.1.1 in    Runtime and configuration
                          generated project       

  Web                     Spring MVC REST         HTTP API

  Build                   Maven                   Dependencies, tests, packaging

  Validation              Jakarta Bean Validation Request validation

  Persistence             Spring Data JPA +       ORM and repositories
                          Hibernate               

  Database                PostgreSQL              Relational persistence

  Migrations              Flyway                  Versioned schema changes

  Security                Spring Security + JWT,  Authentication/authorization
                          planned                 

  Tests                   JUnit, Mockito, Spring  Unit and API tests
                          Boot test support       

  Integration tests       PostgreSQL;             Database-backed verification
                          Testcontainers later    

  API docs                Springdoc OpenAPI,      API exploration
                          planned                 

  Version control         Git + GitHub            Source control

  CI                      Jenkins                 Automated build and tests

  Frontend                React + TypeScript,     User interface
                          later                   

  Payment provider        Local deterministic     Simulated outcomes
                          mock                    
  ------------------------------------------------------------------------------

**Version note:** The generated POM showed a Java 21 target, while a
startup log used Java 27. Verify the JDK used by IntelliJ/Maven and keep
it compatible with the compiler target.

## 4. Architecture

### Style

A **modular monolith**: one Spring Boot application, organized into
feature modules. This is easier to learn and test than starting with
microservices.

### High-level flow

``` text
React + TypeScript frontend (later)
                 | HTTP / JSON
                 v
        Spring Boot REST API
                 v
       Controllers + Request DTOs
                 v
          Application Services
           /       |        \
          v        v         v
   Repositories  Provider   Domain rules
          |       adapter    |
          v                  v
      PostgreSQL       Payment lifecycle,
                       idempotency, ledger,
                       reconciliation

GitHub --> Jenkins --> Maven build and tests
```

Jenkins is part of the development/delivery workflow, not the runtime
payment request path.

### Request lifecycle

1.  Client sends an HTTP request.
2.  Embedded web server receives it.
3.  Spring MVC matches path and HTTP method to a handler.
4.  Controller accepts the request and validation is applied.
5.  Service enforces business rules and coordinates the operation.
6.  Repository reads/writes entities when required.
7.  Controller returns a response DTO.
8.  Spring's HTTP message converter serializes the response, typically
    as JSON.

### Layer responsibilities

  Layer              Responsibility
  ------------------ ---------------------------------------------------------
  Controller         HTTP mapping and request/response handling
  Request DTO        Permitted input fields and validation
  Service            Business rules and orchestration
  Entity             Persisted domain state
  Repository         Persistence operations
  Provider adapter   Isolates provider behavior from payment logic
  Common             Shared configuration, error response, reusable concerns

### Planned package structure

Add modules incrementally. This is a target structure, not a demand to
create every file now.

``` text
src/main/java/com/playflow/playflow1/
├── Playflow1Application.java
├── common/
│   ├── config/
│   ├── exception/
│   └── response/
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── dto/
│   └── security/
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   ├── User.java
│   └── dto/
├── account/
│   ├── AccountController.java
│   ├── AccountService.java
│   ├── AccountRepository.java
│   ├── Account.java
│   └── dto/
├── payment/
│   ├── PaymentController.java
│   ├── PaymentService.java
│   ├── PaymentRepository.java
│   ├── Payment.java
│   ├── PaymentStatus.java
│   └── dto/
├── provider/
│   ├── PaymentProvider.java
│   └── MockPaymentProvider.java
├── idempotency/
│   ├── IdempotencyService.java
│   ├── IdempotencyRepository.java
│   └── IdempotencyRecord.java
├── ledger/
│   ├── LedgerService.java
│   ├── LedgerRepository.java
│   └── LedgerEntry.java
└── reconciliation/
    ├── ReconciliationController.java
    ├── ReconciliationService.java
    └── ReconciliationRecord.java

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__initial_schema.sql
```

## 5. REST API Contract (Planned)

Base path: `/api/v1`. These are target endpoints; only the health
endpoint is confirmed implemented.

### Health

  Method   Path               Purpose                 Status
  -------- ------------------ ----------------------- -------------
  GET      `/api/v1/health`   Basic health response   **Working**

### Authentication and users

  Method   Path                      Purpose
  -------- ------------------------- ---------------------------------
  POST     `/api/v1/auth/register`   Register user
  POST     `/api/v1/auth/login`      Authenticate user
  GET      `/api/v1/users/me`        Current user's profile
  PATCH    `/api/v1/users/me`        Update permitted profile fields

### Accounts

  Method   Path                                     Purpose
  -------- ---------------------------------------- --------------------------
  POST     `/api/v1/accounts`                       Create simulated account
  GET      `/api/v1/accounts`                       List user's accounts
  GET      `/api/v1/accounts/{accountId}`           Get account details
  GET      `/api/v1/accounts/{accountId}/balance`   Get balance

### Payments and transfers

  --------------------------------------------------------------------------------
  Method                  Path                             Purpose
  ----------------------- -------------------------------- -----------------------
  POST                    `/api/v1/payments`               Initiate payment

  GET                     `/api/v1/payments/{paymentId}`   Get payment and status

  GET                     `/api/v1/payments`               Paginated payment list

  POST                    `/api/v1/transfers`              Transfer simulated
                                                           funds between accounts

  GET                     `/api/v1/transactions`           Transaction history
  --------------------------------------------------------------------------------

### Reconciliation

  -----------------------------------------------------------------------------------------------------
  Method                  Path                                                  Purpose
  ----------------------- ----------------------------------------------------- -----------------------
  POST                    `/api/v1/reconciliation/runs`                         Start reconciliation

  GET                     `/api/v1/reconciliation/runs/{runId}`                 Get run summary

  GET                     `/api/v1/reconciliation/runs/{runId}/discrepancies`   List mismatches
  -----------------------------------------------------------------------------------------------------

### HTTP method guidelines

  Annotation         Method   Typical purpose
  ------------------ -------- ---------------------------
  `@GetMapping`      GET      Retrieve data
  `@PostMapping`     POST     Create/initiate operation
  `@PutMapping`      PUT      Replace representation
  `@PatchMapping`    PATCH    Partial update
  `@DeleteMapping`   DELETE   Delete where appropriate

Use `@RequestMapping` on a controller for a shared path prefix and
specialized mappings on handler methods.

### Status codes

  Status   Meaning
  -------- ----------------------------------------------------
  200      Successful response
  201      Resource created
  202      Accepted for processing
  204      Success without body
  400      Malformed/invalid request
  401      Missing/invalid authentication
  403      Authenticated but not permitted
  404      Resource not found
  409      Conflict, including incompatible idempotency reuse
  500      Unexpected server error

Choose one consistent validation-error convention. Do not use status
codes arbitrarily.

### Pagination

Example: `GET /api/v1/payments?page=0&size=20&sort=createdAt,desc`. Cap
maximum page size.

## 6. DTOs and API Boundaries

A **DTO (Data Transfer Object)** defines data transferred across a
boundary, such as between a client and backend.

Do not expose JPA entities directly by default: - Avoid leaking internal
fields. - Prevent clients setting server-owned fields such as status or
provider references. - Keep API contracts stable when database structure
changes. - Make validation explicit. - Avoid accidental serialization of
entity relationships.

Illustrative request DTO:

``` java
public record CreatePaymentRequest(
    Long sourceAccountId,
    Long destinationAccountId,
    BigDecimal amount
) {}
```

Illustrative response DTO:

``` java
public record PaymentResponse(
    Long paymentId,
    BigDecimal amount,
    String status
) {}
```

The service must still validate ownership, account status, amount,
currency, available funds, and allowed state transitions. DTO validation
alone is not business validation.

Use Jakarta Bean Validation (`@NotNull`, `@Positive`, etc.) and trigger
it using `@Valid` in controller parameters where appropriate.

## 7. Spring MVC Learning Notes

### `@Controller` vs `@RestController`

-   `@Controller` marks an MVC controller and is commonly used with
    server-rendered views. A returned string is normally treated as a
    view name unless response-body behavior is requested.
-   `@RestController` combines `@Controller` and `@ResponseBody`;
    handler return values are written to the response body and converted
    by an HTTP message converter, commonly to JSON.
-   PayFlow uses `@RestController` for its JSON API.

### `@RequestMapping` vs `@GetMapping`

-   `@RequestMapping` is a general mapping annotation. It can define
    path and conditions such as HTTP method, headers, and content type.
-   `@GetMapping` is a shortcut for a GET mapping.
-   Class-level and method-level paths combine to form the route.

Example:

``` java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    @GetMapping("/{paymentId}")
    public String getPayment() {
        return "Retrieve payment";
    }
}
```

In a real implementation, bind the path with `@PathVariable`.

### Confirmed health controller

``` java
package com.playflow.playflow1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    @GetMapping
    public Map<String, String> checkHealth() {
        return Map.of(
            "status", "UP",
            "application", "PayFlow",
            "message", "PayFlow is running!"
        );
    }
}
```

Test:

``` bash
curl -i http://localhost:8080/api/v1/health
```

This proves the HTTP application responds; it does not prove PostgreSQL
is reachable.

## 8. Database Design

### Planned tables

  -----------------------------------------------------------------------
  Table                               Purpose
  ----------------------------------- -----------------------------------
  `users`                             Identity, email, password hash,
                                      timestamps

  `accounts`                          User-owned simulated accounts,
                                      currency, status

  `payments`                          Payment operation, amount, status,
                                      accounts, timestamps

  `idempotency_records`               Key, request fingerprint, stored
                                      result/reference

  `provider_attempts`                 Attempt, provider reference,
                                      outcome, timing

  `ledger_entries`                    Auditable debit/credit records

  `reconciliation_runs`               Run metadata and summary

  `reconciliation_discrepancies`      Detected mismatches
  -----------------------------------------------------------------------

### Relationship outline

-   One user can own multiple accounts.
-   A payment references source and, for transfers, destination
    accounts.
-   A payment may have multiple provider attempts if retry/recovery is
    supported.
-   Financial operations may have multiple ledger entries.
-   A reconciliation run can contain multiple discrepancy records.

### Money and currency

-   Java `BigDecimal`; PostgreSQL `NUMERIC` with deliberate precision
    and scale.
-   Never use `float` or `double` for money.
-   Store currency explicitly and reject unsupported currency
    combinations.
-   Define rounding and scale rules.

### Persistence

-   PostgreSQL is the planned database.
-   Spring Data JPA provides repository abstractions; Hibernate
    implements ORM behavior.
-   Flyway manages versioned SQL migrations.
-   Do not treat DTOs as entities or assume an object is persisted
    before a successful repository/transaction operation.

### Illustrative `application.yml`

Configure environment variables and adapt the settings to the actual
generated project and dependencies.

``` yaml
spring:
  application:
    name: payflow
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/payflow}
    username: ${DB_USERNAME:payflow}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

server:
  port: 8080
```

Before using it: 1. Create the local PostgreSQL database and user. 2.
Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. 3. Verify PostgreSQL
JDBC and Flyway dependencies. 4. Create the first migration. 5. Confirm
startup and a database-backed test.

Use Flyway to define the schema. `ddl-auto: validate` is appropriate
once migrations define the schema. Do not switch blindly to automatic
schema creation to hide migration problems.

### Transactions

Operations such as a transfer may need to record the operation and
balanced ledger entries atomically. Use service-level transaction
boundaries. A database transaction cannot atomically include a remote
provider; that boundary requires separate recovery logic.

## 9. Payment Processing

### Conceptual state machine

``` text
CREATED -> PROCESSING -> SUCCEEDED
                      -> FAILED
                      -> UNKNOWN
```

Allowed transitions will be enforced by the service. `UNKNOWN` means a
timeout or uncertain result where the provider may have processed the
request. It must not be treated automatically as a definite failure.

### Initiation flow

1.  Client sends payment request and idempotency key.
2.  Controller parses and validates the request.
3.  Service checks authorization and business rules.
4.  Service checks idempotency.
5.  Payment operation is recorded according to the chosen processing
    strategy.
6.  Mock provider returns a controlled outcome.
7.  Service updates state and records appropriate ledger effects.
8.  API returns approved response fields.

Avoid holding a database transaction open across a slow external network
call in any future real-provider integration.

### Provider abstraction

Create a `PaymentProvider` interface and deterministic
`MockPaymentProvider`. Support test-controlled success, definite
failure, timeout/unknown result, and optionally delayed response/status
lookup. Payment business logic should depend on the interface, not
provider-specific code.

### Idempotency

-   Same key + same logical request: return the existing
    operation/result.
-   Same key + different payload: reject with conflict.
-   Concurrent requests must not create duplicate operations.
-   Store a request fingerprint or equivalent comparison data.
-   Enforce uniqueness in the database; a service check alone is
    insufficient under concurrency.
-   Document key scope, retention, and replay behavior.
-   An `Idempotency-Key` header is a candidate convention to formalize.

### Ledger

Ledger entries form an audit trail and should be append-only in normal
operation. Define a debit/credit convention and verify balanced entries
for the same currency and operation. Decide whether balances are derived
from the ledger or materialized separately. Corrections should use
deliberate reversal/correction entries if supported, not silent edits to
history.

### Reconciliation

Compare internal records with simulated provider records. Possible
discrepancy types: - Internal payment missing from provider records. -
Provider record missing internally. - Amount/currency mismatch. - Status
mismatch. - Duplicate provider reference. - Payment unknown beyond a
threshold.

Store run timing, summary counts, and discrepancy records. Start
synchronously against a small mock dataset; add scheduling only when
justified.

## 10. Security, Errors, and Logging

### Security baseline

-   Hash passwords with an established password encoder.
-   Use Spring Security for authentication and authorization.
-   Introduce JWT after the basic request flow is understood.
-   Verify ownership for every account/payment lookup; never trust an ID
    alone.
-   Validate on the server.
-   Keep secrets out of source control.
-   Never log passwords, tokens, or sensitive payment data.
-   Do not expose stack traces/internal provider details to clients.
-   Restrict simulation/admin controls to authorized users.

### Error handling

Use `@RestControllerAdvice` to produce consistent error bodies.

Illustrative shape:

``` json
{
  "timestamp": "2026-10-22T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/payments"
}
```

Define one final schema and timestamp convention. Avoid leaking internal
exception details.

### Logging

Log useful events such as safe correlation IDs, state transitions,
provider outcomes, and reconciliation summaries. Never log secrets or
sensitive request bodies.

## 11. Testing Strategy

  -----------------------------------------------------------------------
  Test type               Purpose                 Example
  ----------------------- ----------------------- -----------------------
  Unit                    Business logic without  State transitions,
                          full app                idempotency

  Controller/API          HTTP contract           Status codes, JSON,
                                                  validation

  Repository              Persistence             Unique keys,
                                                  relationships, queries

  Integration             Components together     Rollback, payment
                                                  persistence, provider
                                                  outcome

  End-to-end              Complete user journey   Register, account,
                                                  payment, history
  -----------------------------------------------------------------------

Checklist: - Health endpoint returns 200 and expected JSON. - Invalid
payment request is rejected. - Invalid state transitions are blocked. -
Duplicate idempotent request creates no duplicate. - Same key with
different payload is rejected. - Local database failure leaves no
partial ledger effects. - Failed/unknown provider outcomes do not
produce incorrect successful ledger effects. - Reconciliation identifies
seeded discrepancies. - Users cannot access other users' records. -
`mvn clean test` succeeds locally and in Jenkins.

Start with the testing dependencies already generated by Spring
Initializr. Use JUnit/Mockito and Spring MVC test support. Add
Testcontainers for PostgreSQL integration tests when persistence exists.

## 12. Git Workflow

Suggested branches: - `main`: stable tested baseline. -
`feature/health-controller` - `feature/payment-api` -
`feature/postgres-persistence` - `feature/idempotency` -
`feature/reconciliation` - `ci/jenkins-pipeline`

Suggested commit messages: - `feat: add health endpoint` -
`feat: add payment request DTO` - `test: cover payment validation` -
`build: add Jenkins pipeline`

Commit coherent changes, test before pushing, and do not commit IDE-only
local files, secrets, build output, or database dumps. Use a
Java/Maven/IntelliJ-appropriate `.gitignore`.

## 13. Jenkins CI/CD

### Initial pipeline

``` text
GitHub push / pull request
          -> Jenkins checkout
          -> Verify Java/Maven
          -> mvn clean test
          -> mvn package
          -> Publish test/build status
```

Introduce CI after the first endpoint and a small automated test suite
work. Make sure Jenkins does not depend on a developer's machine or
local secrets. If integration tests need PostgreSQL, provision a test
database or use Testcontainers.

-   **CI:** automatically build and test changes.
-   **CD:** prepare or deploy validated artifacts to an environment.

Start with CI; deploy only after configuration and tests are reliable.

## 14. Roadmap

### Phase 1 --- REST fundamentals

-   [x] Generate Spring Boot Maven project.
-   [x] Implement `HealthController`.
-   [x] Confirm `GET /api/v1/health` returns JSON.
-   [ ] Understand controller annotations and routing.
-   [ ] Learn DTOs, validation, status codes.
-   [ ] Add centralized exception handling.
-   [ ] Add basic API tests.

### Phase 2 --- Database foundation

-   [ ] Verify JDK/Maven compatibility.
-   [ ] Configure PostgreSQL.
-   [ ] Configure datasource in `application.yml`.
-   [ ] Verify PostgreSQL driver and Flyway dependencies.
-   [ ] Create initial schema migration.
-   [ ] Add JPA repositories/entities.
-   [ ] Pass a database-backed test.

### Phase 3 --- User and account foundation

-   [ ] Define user/account entities.
-   [ ] Add repositories, services, DTOs and APIs.
-   [ ] Add validation and ownership rules.
-   [ ] Implement authentication/authorization.

### Phase 4 --- Payment core

-   [ ] Define payment state machine.
-   [ ] Implement payment DTOs, service, repository.
-   [ ] Add mock provider.
-   [ ] Implement idempotency.
-   [ ] Define ledger and transaction rules.
-   [ ] Test success, failure, timeout, and duplicates.

### Phase 5 --- CI

-   [ ] Create/push GitHub repository.
-   [ ] Add Jenkins pipeline.
-   [ ] Run Maven tests and package automatically.
-   [ ] Publish test results.
-   [ ] Manage credentials safely.

### Phase 6 --- Reconciliation

-   [ ] Define provider record format.
-   [ ] Implement reconciliation run/discrepancy models.
-   [ ] Test missing/mismatched/duplicate records.
-   [ ] Add reconciliation APIs.

### Phase 7 --- Frontend and polish

-   [ ] Build React + TypeScript dashboard.
-   [ ] Add account, payment, and history views.
-   [ ] Document APIs with OpenAPI.
-   [ ] Add integration tests and operational docs.
-   [ ] Consider deployment after CI is stable.

## 15. Architecture Decisions and Open Questions

  -----------------------------------------------------------------------------
  Decision                Current choice             Reason
  ----------------------- -------------------------- --------------------------
  Architecture            Modular monolith           Learnable and testable

  API                     Versioned REST + JSON      Clear client/server
                                                     contract

  Root package            `com.playflow.playflow1`   Preserve generated project

  Database                PostgreSQL                 Relational
                                                     constraints/transactions

  Persistence             Spring Data JPA +          Learn ORM and repositories
                          Hibernate                  

  Migrations              Flyway                     Versioned schema evolution

  Money                   `BigDecimal` + PostgreSQL  Avoid floating-point
                          `NUMERIC`                  errors

  API boundaries          DTOs                       Separate API contract from
                                                     entities

  Provider                Deterministic mock         Safe, repeatable failure
                                                     testing

  CI                      GitHub + Jenkins + Maven   Automate build/tests early

  Frontend                React + TypeScript, later  Focus first on backend

  Distributed             Deferred                   Avoid unjustified
  infrastructure                                     complexity
  -----------------------------------------------------------------------------

Resolve these during implementation: - Exact payment/transfer semantics
and when payment becomes final. - Derived vs materialized account
balances. - Idempotency key scope, retention, and replay response. -
Resolution strategy for `UNKNOWN` provider outcomes. - Error response
schema and validation status convention. - Token lifetime/refresh
policy. - Reconciliation matching and discrepancy-resolution rules.

## 16. Glossary

  Term             Meaning
  ---------------- ----------------------------------------------------------------
  REST API         HTTP interface exposed to clients
  Controller       Handles HTTP mapping and response behavior
  Service          Coordinates business logic
  DTO              Data Transfer Object crossing an application boundary
  Entity           Object mapped to persisted database state
  Repository       Persistence abstraction
  ORM              Maps objects to relational tables
  JPA              Java persistence specification; Hibernate is an implementation
  Migration        Versioned database schema change
  Idempotency      Safe request replay without duplicating the operation
  Ledger           Auditable record of financial effects
  Reconciliation   Comparison of internal and independent records
  CI               Automated build/test verification
  CD               Automated preparation or deployment of validated software

## 17. Learning Notes Template

Use this for each new topic:

### Topic

-   **Definition:**
-   **Why PayFlow needs it:**
-   **Key annotations/classes:**
-   **Request/data flow:**
-   **Implementation:**
-   **How I tested it:**
-   **Common mistakes:**
-   **Interview questions:**
-   **What I still need to understand:**

## 18. Project Principles

1.  Understand a feature before adding abstractions.
2.  Build incrementally; do not create every planned package at once.
3.  Keep HTTP concerns in controllers and business rules in services.
4.  Separate DTOs from persistence entities.
5.  Make failure behavior explicit and testable.
6.  Treat idempotency, authorization, and transaction boundaries as
    correctness requirements.
7.  Never claim persistence works until tested against PostgreSQL.
8.  Run tests before merging or marking a phase complete.
9.  Keep secrets out of source control.
10. Add infrastructure only when a concrete requirement calls for it.

**Next step:** continue controller/request-mapping/DTO learning, then
add an API test for the health endpoint. Configure PostgreSQL before
introducing database-backed features.
