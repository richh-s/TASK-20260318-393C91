# City Bus Operation and Service Coordination Platform — Technical Design

## 1. System Overview

A full-stack web platform for urban bus operations. Three personas interact with the system:

- **Passenger** — searches routes/stops, makes reservations, receives arrival reminders and missed-check-in alerts.
- **Dispatcher** — creates and manages operational workflow tasks; approves or rejects tasks raised by other dispatchers.
- **Admin** — configures notification templates, sorting weights, field dictionaries, system config, and imports bus data.

The platform is entirely self-contained and started with `docker compose up`. It requires no external message broker or cloud service.

---

## 2. Architecture

```
┌────────────────────────────────────────────────────────┐
│                    Browser (React SPA)                  │
│  Login │ Passenger Dashboard │ Dispatcher │ Admin       │
└───────────────────────┬────────────────────────────────┘
                        │ HTTPS / REST JSON
┌───────────────────────▼────────────────────────────────┐
│               Spring Boot 3.2.3 (port 8080)             │
│  Controllers → Services → Repositories                  │
│  JwtAuthenticationFilter (MDC traceId injection)        │
│  @EnableScheduling  @EnableAsync                        │
└───────────┬───────────────────────────┬────────────────┘
            │ JDBC / Flyway             │ (same DB)
┌───────────▼──────────┐    ┌───────────▼───────────────┐
│  PostgreSQL 16        │    │  message_queue table        │
│  (port 5432)          │    │  (DB-backed async queue)    │
└──────────────────────┘    └───────────────────────────┘
```

**Technology choices:**

| Concern | Choice | Reason |
|---------|--------|--------|
| Runtime | Java 17 LTS | LTS, modern records/sealed types |
| Framework | Spring Boot 3.2.3 | Auto-config, actuator, scheduler, async |
| Auth | JJWT 0.12.x + BCrypt | Stateless JWT, no session storage |
| DB | PostgreSQL 16 | JSONB support, reliability |
| Migrations | Flyway | Version-controlled schema, reproducible |
| Frontend | React 18 + Vite + TypeScript | Fast build, type safety |
| UI library | Ant Design 5 | Rich data tables, forms, notifications |
| Container | Docker multi-stage build | Small JRE-only final image |
| Queue | DB-backed (message_queue table) | No external MQ; works offline/LAN |

---

## 3. Modules

| Module | Package | Responsibility |
|--------|---------|----------------|
| Auth | `security`, `service.AuthService` | Register, login, JWT issue/validate |
| Search | `service.SearchService` | Route/stop search with pinyin, weighted sort |
| Reservation | `service.ReservationService` | Create/cancel/list reservations |
| Notification | `service.NotificationService` | Create, read, preference/DND management |
| Workflow | `service.WorkflowService` | Task lifecycle + approval chain |
| Admin | `service.AdminService` | Templates, weights, dictionaries, config |
| Bus Data Import | `service.BusDataService` | Async JSON/HTML file import |
| Message Queue | `service.MessageQueueService` | DB-backed deferred messaging |
| Scheduler | `scheduler.NotificationScheduler` | Cron/fixed-delay background jobs |

---

## 4. Domain Model

### 4.1 Core entities

```
User
  id, username, passwordHash, displayName
  role: PASSENGER | DISPATCHER | ADMIN
  createdAt

BusRoute
  id, routeNumber, nameEn, nameCn
  description, status: ACTIVE | INACTIVE | SUSPENDED
  createdAt

BusStop
  id, nameEn, nameCn, address
  pinyin, pinyinInitials
  sequenceNumber, popularityScore
  route → BusRoute
  createdAt

Reservation
  id, user → User, route → BusRoute, stop → BusStop
  scheduledTime, status: CONFIRMED | CANCELLED | MISSED
  createdAt

Notification
  id, user → User
  type: ARRIVAL_REMINDER | MISSED_CHECKIN | RESERVATION_SUCCESS |
        RESERVATION_CANCELLED | TASK_ASSIGNED | TASK_ESCALATED
  title, content, read, entityId
  createdAt

NotificationPreference
  id, user → User
  route → BusRoute (nullable), stop → BusStop (nullable)
  reminderMinutes, enabled
  dndEnabled, dndStart (LocalTime), dndEnd (LocalTime)

WorkflowTask
  id, taskNumber (e.g. RDC-20260421-1001)
  type: ROUTE_CHANGE | DRIVER_COMPLAINT | SCHEDULE_ADJUSTMENT |
        INCIDENT_REPORT | MAINTENANCE_REQUEST
  title, description
  status: PENDING | APPROVED | REJECTED | RETURNED | ESCALATED
  assignedTo → User, createdBy → User
  deadline, escalated (boolean), payload (JSONB)
  createdAt, updatedAt

WorkflowApproval
  id, task → WorkflowTask, approver → User
  action: APPROVE | REJECT | RETURN
  comment, createdAt

NotificationTemplate
  id, name, type (NotificationType)
  titleTemplate, contentTemplate
  sensitivityLevel (int: 0=none, 1=low, 2=high)
  createdAt

SortingWeight
  id, factorName, weight (double)

FieldDictionary
  id, fieldName, rawValue, standardValue

SystemConfig
  id, configKey, configValue, description

MessageQueueItem
  id, type, payload (JSONB)
  status: PENDING | PROCESSING | DONE | FAILED
  retryCount, scheduledAt, processedAt

BusDataImport
  id, importType: JSON | HTML, fileName
  status: PENDING | PROCESSING | DONE | FAILED
  rowsParsed, rowsFailed
  importedBy → User, createdAt

AuditLog
  id, entityType, entityId, action, actorId
  oldValue (JSONB), newValue (JSONB), createdAt
```

### 4.2 State transitions

**Reservation:**
```
CONFIRMED ──cancel()──→ CANCELLED
CONFIRMED ──detectMissedCheckins()──→ MISSED
```

**WorkflowTask:**
```
PENDING ──APPROVE──→ APPROVED
PENDING ──REJECT───→ REJECTED
PENDING ──RETURN───→ RETURNED
PENDING/APPROVED/RETURNED ──escalateOverdueTasks()──→ ESCALATED
```

**MessageQueueItem:**
```
PENDING ──processPending()──→ PROCESSING ──success──→ DONE
                                          ──failure (retries<3)──→ PENDING (backoff)
                                          ──failure (retries≥3)──→ FAILED
```

**BusDataImport:**
```
PENDING ──processAsync()──→ PROCESSING ──success──→ DONE
                                        ──exception──→ FAILED
```

---

## 5. Search Design

Stop search uses four stored columns to support both English and Chinese queries without runtime libraries:

- `name_en` — English name (plain text)
- `name_cn` — Chinese name (UTF-8)
- `pinyin` — space-separated full pinyin e.g. `zhongshan lu`
- `pinyin_initials` — initials only e.g. `zsl`

**Weighted sort score:**
```
sortScore = popularityScore × popWeight
```
Where `popWeight` is read from the `sorting_weights` table (factorName = `popularity`).

**Autocomplete** returns the top-N combined results across routes and stops, filtered by the query against `name_en ILIKE`, `name_cn LIKE`, `pinyin ILIKE`, and `pinyin_initials ILIKE`.

---

## 6. Security and Permission Boundaries

| Endpoint pattern | Permitted roles |
|-----------------|-----------------|
| `POST /api/auth/**` | Anonymous |
| `GET /api/search/**` | Any authenticated |
| `GET /api/notifications/**`, `PATCH /api/notifications/**` | PASSENGER |
| `GET /api/notifications/preferences`, `POST /api/notifications/preferences` | PASSENGER |
| `POST /api/reservations`, `GET /api/reservations`, `DELETE /api/reservations/**` | PASSENGER |
| `POST /api/workflow/**`, `GET /api/workflow/**` | DISPATCHER |
| `GET /api/admin/**`, `POST /api/admin/**`, `DELETE /api/admin/**` | ADMIN |
| `GET /api/bus-data/**`, `POST /api/bus-data/**` | ADMIN |
| `GET /actuator/**` | Any authenticated |

JWT is validated on every request via `JwtAuthenticationFilter`. The filter also injects a random `traceId` into MDC for request-scoped log correlation.

Ownership invariants enforced in services (not SecurityConfig):
- Reservation cancel: `reservation.user.id == authenticatedUserId`
- Notification read: `notification.user.id == authenticatedUserId`

---

## 7. Notification and DND Logic

DND windows are stored as `LocalTime` pairs (start, end) and support midnight-crossing:

```
isInDnd(current, start, end):
  if start < end:  current > start AND current < end
  else:            current > start OR  current < end   // crosses midnight
```

Notification delivery is suppressed for a reservation if **any** matching preference has DND active at the current time.

Content masking applies to `TASK_ESCALATED` notifications — phone numbers (`\d{3,4}-\d{7,8}` or 11-digit mobile) are replaced with `***`.

---

## 8. Async and Scheduling

All background tasks run within the single Spring Boot process:

| Job | Trigger | Action |
|-----|---------|--------|
| `sendArrivalReminders` | Every 60 s (configurable) | Finds CONFIRMED reservations in `[now, now+12min]`; sends ARRIVAL_REMINDER if past reminderTime and not in DND |
| `detectMissedCheckins` | Every 120 s (configurable) | Finds CONFIRMED reservations older than 5 min; marks MISSED, sends MISSED_CHECKIN |
| `escalateOverdueTasks` | Top of every hour (cron) | Marks PENDING tasks older than 24 h as ESCALATED; sends TASK_ESCALATED (masked) |
| `processMessageQueue` | Every 30 s (configurable) | Processes PENDING queue items; max 3 retries with 5-min backoff |

`BusDataService.processAsync` runs on Spring's default async thread pool (`@Async`).

---

## 9. Bus Data Import

Supports two import formats:

**JSON**: `[{"routeNumber":"...", "stopName":"...", "sequence":1, ...}]`

**HTML**: Table parsed with regex:
- Row pattern: `<tr[^>]*>(.*?)</tr>`
- Cell pattern: `<td[^>]*>(.*?)</td>`
- Tag strip pattern: `<[^>]+>`

Field values are normalised via `FieldDictionary` (rawValue → standardValue lookup) before upsert. If a matching BusStop exists (by routeId + nameEn), it is updated; otherwise created.

---

## 10. Docker Execution Assumptions

- The system is started exclusively via `docker compose up` from `repo/`.
- PostgreSQL is on the `citybus` network, hostname `postgres`, port 5432.
- Backend waits for `postgres` health check (`pg_isready`) before starting.
- Frontend is served by nginx on port 3000; all `/api` requests are proxied to `backend:8080`.
- Flyway runs on application startup and applies `V1__init_schema.sql` then `V2__seed_data.sql`.
- All credentials are injected via environment variables with safe local defaults.

---

## 11. Logging and Observability

- Structured log line: `timestamp [thread] [traceId=<uuid>] LEVEL logger - message`
- `traceId` is injected by `JwtAuthenticationFilter` at request entry and cleared on exit.
- Actuator endpoints exposed: `health`, `info`, `metrics`, `prometheus`.
- Prometheus tag: `application=citybus-platform`.
- Key custom metrics (via Micrometer Counter/Timer):
  - `notifications.sent` (tags: type)
  - `reservations.created`
  - `workflow.tasks.escalated`
  - `bus_data.import.rows_parsed`, `bus_data.import.rows_failed`

---

## 12. Testing Strategy

### Unit tests (`unit_tests/`)
- Framework: JUnit 5 + Mockito
- Scope: Service layer only; all dependencies mocked
- Location: `unit_tests/src/test/java/com/citybus/platform/`
- Coverage target: ≥ 90% of public service methods
- Uses `application-test.yml` (H2 in-memory, no PostgreSQL required)
- Test classes: `AuthServiceTest`, `SearchServiceTest`, `ReservationServiceTest`, `NotificationServiceTest`, `WorkflowServiceTest`, `AdminServiceTest`, `MessageQueueServiceTest`, `NotificationSchedulerTest`

### API / integration tests (`API_tests/`)
- Framework: Spring Boot `@SpringBootTest` + MockMvc + H2
- Scope: Full controller → service → repository stack
- Coverage target: ≥ 90% of REST endpoint surface
- Test classes: `AuthApiTest`, `SearchApiTest`, `ReservationApiTest`, `NotificationApiTest`, `WorkflowApiTest`, `AdminApiTest`, `BusDataApiTest`

### Canonical test entrypoint
```bash
./run_tests.sh    # runs unit_tests then API_tests; exits non-zero on failure
```

---

## 13. Frontend Module Map

| Page / Component | Route | Role |
|-----------------|-------|------|
| LoginPage | `/login` | anon |
| PassengerDashboard | `/passenger` | PASSENGER |
| — RouteSearch | `/passenger/search` | PASSENGER |
| — Reservations | `/passenger/reservations` | PASSENGER |
| — MessageCenter | `/passenger/messages` | PASSENGER |
| — NotificationSettings | `/passenger/settings` | PASSENGER |
| DispatcherDashboard | `/dispatcher` | DISPATCHER |
| — TaskList | `/dispatcher/tasks` | DISPATCHER |
| — TaskDetail | `/dispatcher/tasks/:id` | DISPATCHER |
| AdminDashboard | `/admin` | ADMIN |
| — Templates | `/admin/templates` | ADMIN |
| — SortingWeights | `/admin/weights` | ADMIN |
| — FieldDictionary | `/admin/dictionaries` | ADMIN |
| — SystemConfig | `/admin/configs` | ADMIN |
| — BusDataImport | `/admin/imports` | ADMIN |

State management: React Context for auth (token + user role). API calls use `fetch` with the JWT `Authorization: Bearer <token>` header. Role-based `<PrivateRoute>` components redirect unauthenticated or under-privileged users to `/login`.
