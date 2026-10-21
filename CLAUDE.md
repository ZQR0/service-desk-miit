# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
Service Desk and service catalog for teachers. A multi-module monolith built with Spring Boot 4 (Java 21, Maven) and PostgreSQL.

## Architecture
The project is structured as a multi-module monolith where each module represents a domain. Modules communicate via public interfaces (synchronous) or Spring Application Events (asynchronous).

### Module Hierarchy & Dependencies
- `common`: General utilities, error formats. No business logic. (Depends on: none)
- `auth`: Users, departments, security configuration, `CurrentUser` context. (Depends on: `common`)
- `catalog`: Service catalog, checklist templates. (Depends on: `common`, `auth`)
- `requests`: Requests, approval flows, statuses, checklists, SLA statistics. (Depends on: `common`, `auth`, `catalog`)
- `notifications`: Notifications, SSE. (Depends on: `common`, `auth`, `requests` for events)
- `app`: Entry point, global configuration, JAR assembly. (Depends on: all modules)

### Module Internal Structure
Each module follows a strict visibility pattern:
- **Public API** (Root package: `rut.miit.servicedesk.<module>/`): Interfaces, DTOs, Enums, and Events. Only these are accessible to other modules.
- **Internal Implementation** (`.../<module>/internal/`): Entities, Repositories, Service implementations, and Controllers. These are package-private and must not be accessed from outside the module.

### Key Architectural Rules
- **No JPA Links Between Modules**: Entities in different modules must not have `@ManyToOne` or `@OneToMany` relationships. Use IDs (e.g., `userId`, `serviceId`) and fetch data via the corresponding module's public interface.
- **No Cross-Module JOINs**: JPQL or native queries must not perform JOINs with tables belonging to other modules.
- **DTOs Only**: Entities must never leave the `internal` package. Always map to DTOs or records for public methods.
- **Event-Driven Notifications**: Modules publish events; the `notifications` module listens to them. Listeners must use `@Async` and `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
- **Data Snapshots**: To avoid excessive cross-module calls and maintain history, snapshot critical data (e.g., `departmentId` of the author) at the time of request creation.

## Development Commands

### Build and Run
- **Full Build**: `mvn clean package`
- **Run Application**: 
  ```bash
  DB_URL=jdbc:postgresql://localhost:5432/servicedesk DB_USER=... DB_PASSWORD=... java -jar app/target/service-desk.jar
  ```
- **Docker Setup (Dependencies only)**: `docker compose up -d postgres keycloak`
- **Full Docker Launch**: `docker compose up --build`

### Testing
- **Run All Tests**: `mvn verify`
- **Run Single Test**: `mvn test -Dtest=<TestClassName>`
- **Architectural Validation**: The project uses ArchUnit to enforce module boundaries. Ensure `mvn verify` passes to check for boundary violations.

## Security & Authentication
- **Provider**: Keycloak (OAuth2/JWT).
- **Roles**: `TEACHER`, `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF`, `DUTY_OFFICER`, `ADMIN`.
- **Context**: Use the `CurrentUser` interface from the `auth` module to access the authenticated user's local ID and department.
- **Authorization**: Use `@PreAuthorize("hasRole('...')")` on controllers/services. Always verify data ownership (e.g., check if the request belongs to the user's department).
