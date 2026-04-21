# City Bus Operation and Service Coordination Platform

A full-stack platform for urban bus operations: passenger reservations, dispatcher workflow management, admin configuration, and automated notification scheduling.

## Quick Start

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend (React) | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| API Health | http://localhost:8080/actuator/health |
| Metrics (Prometheus) | http://localhost:8080/actuator/prometheus |

First startup takes ~2 minutes for Maven to download dependencies and build the JAR. Subsequent starts are faster.

## Seed Accounts

After startup, the following test accounts are available (from `V2__seed_data.sql`):

| Username | Password | Role |
|----------|----------|------|
| passenger1 | password | PASSENGER |
| dispatcher1 | password | DISPATCHER |
| admin | password | ADMIN |

## Running Tests

```bash
./run_tests.sh
```

This runs unit tests (JUnit 5 + Mockito) then API/integration tests (Spring MockMvc + H2). Requires Java 17 and Maven on the host.

To run test suites individually:

```bash
# Unit tests only
cd unit_tests && mvn test

# API integration tests only
cd API_tests && mvn test
```

## Project Structure

```
repo/
├── backend/                  Spring Boot 3.2.3 application
│   ├── src/main/java/        Application source
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/     Flyway migrations (V1, V2)
│   └── Dockerfile            Multi-stage Maven → JRE image
├── frontend/                 React 18 + Vite + TypeScript + Ant Design 5
│   ├── src/
│   └── Dockerfile            nginx serving built assets
├── unit_tests/               JUnit 5 + Mockito service-layer tests
├── API_tests/                Spring @SpringBootTest + MockMvc integration tests
├── docs/
│   ├── design.md             Architecture, domain model, state transitions
│   └── api-spec.md           Full REST API reference
├── docker-compose.yml
├── run_tests.sh
└── README.md
```

## Architecture Summary

- **Backend**: Spring Boot 3.2.3, Java 17, PostgreSQL 16, Flyway, JJWT 0.12.x
- **Frontend**: React 18, TypeScript, Vite, Ant Design 5
- **Auth**: Stateless JWT (BCrypt passwords, 24-hour expiry)
- **Roles**: PASSENGER / DISPATCHER / ADMIN
- **Queue**: DB-backed message queue (no external broker required)
- **Scheduler**: Spring `@Scheduled` — arrival reminders, missed check-ins, task escalation, queue processing

See [docs/design.md](docs/design.md) for full architecture documentation.
See [docs/api-spec.md](docs/api-spec.md) for the REST API reference.

## Environment Variables

All variables have safe local defaults. Override in `docker-compose.yml` for production:

| Variable | Default | Purpose |
|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/citybus` | DB connection |
| `SPRING_DATASOURCE_USERNAME` | `citybus` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `citybus_pass` | DB password |
| `JWT_SECRET` | *(default dev key — change in prod)* | JWT signing secret |
| `JWT_EXPIRATION` | `86400000` | Token TTL in ms (24 h) |
| `SERVER_PORT` | `8080` | Backend port |
