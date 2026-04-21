# City Bus Platform — REST API Specification

Base URL: `http://localhost:8080`

All responses use the envelope:
```json
{ "success": true, "data": <payload>, "message": null }
{ "success": false, "data": null, "message": "<error description>" }
```

Authentication: `Authorization: Bearer <JWT>` header required on all endpoints except `POST /api/auth/login` and `POST /api/auth/register`.

---

## Auth Module

### POST /api/auth/register
Register a new user.

**Request body:**
```json
{
  "username": "alice",
  "password": "secret123",
  "displayName": "Alice"
}
```
Constraints: `username` not blank, `password` min 8 chars, `displayName` optional.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "token": "<jwt>",
    "userId": 1,
    "username": "alice",
    "role": "PASSENGER",
    "displayName": "Alice"
  }
}
```

**Errors:**
- `400` — validation failure
- `409` — username already exists

---

### POST /api/auth/login
Authenticate and receive a JWT.

**Request body:**
```json
{ "username": "alice", "password": "secret123" }
```

**Response 200:** same shape as register response.

**Errors:**
- `401` — bad credentials
- `400` — validation failure

---

### GET /api/auth/me
Return the authenticated user's profile.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "alice",
    "role": "PASSENGER",
    "displayName": "Alice"
  }
}
```

**Errors:**
- `401` — missing or invalid token

---

## Search Module

All search endpoints require authentication (any role).

### GET /api/search/routes?q={query}
Search bus routes.

**Query params:** `q` (string, required)

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "routeNumber": "101",
      "name": "City Center Loop",
      "description": "...",
      "status": "ACTIVE",
      "stopCount": 12
    }
  ]
}
```

---

### GET /api/search/stops?q={query}&routeId={routeId}
Search bus stops, optionally filtered by route.

**Query params:** `q` (required), `routeId` (optional, Long)

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": 5,
      "nameEn": "Zhongshan Road",
      "nameCn": "中山路",
      "address": "Zhongshan Road, District 1",
      "sequenceNumber": 3,
      "routeId": 1,
      "routeNumber": "101",
      "popularityScore": 88,
      "sortScore": 88.0
    }
  ]
}
```

---

### GET /api/search/autocomplete?q={query}
Combined autocomplete across routes and stops. Returns top-10 matches.

**Query params:** `q` (required)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "routes": [ /* RouteSearchResult[] */ ],
    "stops":  [ /* StopSearchResult[] */ ]
  }
}
```

---

## Reservation Module

Role required: **PASSENGER**

### POST /api/reservations
Create a reservation.

**Request body:**
```json
{
  "routeId": 1,
  "stopId": 5,
  "scheduledTime": "2026-05-01T08:30:00"
}
```
Constraints: `routeId` and `stopId` not null, `scheduledTime` must be in the future. The stop must belong to the route.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "routeId": 1,
    "routeNumber": "101",
    "routeName": "City Center Loop",
    "stopId": 5,
    "stopNameEn": "Zhongshan Road",
    "stopNameCn": "中山路",
    "scheduledTime": "2026-05-01T08:30:00",
    "status": "CONFIRMED",
    "createdAt": "2026-04-21T10:00:00"
  }
}
```

**Errors:**
- `400` — validation or stop/route mismatch
- `404` — route or stop not found

---

### GET /api/reservations?page={p}&size={s}
List the authenticated passenger's reservations, newest first.

**Query params:** standard Spring Pageable (`page`, `size`, `sort`)

**Response 200:** paginated `ReservationResponse[]`

---

### DELETE /api/reservations/{id}
Cancel a reservation.

**Path param:** `id` (Long)

**Response 200:** `{ "success": true, "data": null }`

**Errors:**
- `403` — reservation belongs to another user
- `404` — reservation not found
- `400` — reservation is not CONFIRMED (already cancelled or missed)

---

## Notification Module

Role required: **PASSENGER**

### GET /api/notifications?page={p}&size={s}
List all notifications for the authenticated user, newest first.

**Response 200:** paginated `NotificationResponse[]`
```json
{
  "id": 1,
  "type": "ARRIVAL_REMINDER",
  "title": "Arrival Reminder",
  "content": "Your bus 101 departs in 10 minutes from Zhongshan Road",
  "read": false,
  "entityId": 10,
  "createdAt": "2026-04-21T08:20:00"
}
```

---

### GET /api/notifications/unread-count
**Response 200:** `{ "success": true, "data": 3 }`

---

### PATCH /api/notifications/{id}/read
Mark a single notification as read.

**Errors:**
- `403` — notification belongs to another user
- `404` — not found

---

### PATCH /api/notifications/read-all
Mark all of the authenticated user's notifications as read.

**Response 200:** `{ "success": true, "data": null }`

---

### GET /api/notifications/preferences
List the authenticated user's notification preferences.

**Response 200:** `NotificationPreference[]`
```json
[
  {
    "id": 1,
    "routeId": 1,
    "stopId": 5,
    "reminderMinutes": 10,
    "enabled": true,
    "dndEnabled": false,
    "dndStart": null,
    "dndEnd": null
  }
]
```

---

### POST /api/notifications/preferences
Create or update a notification preference (upsert by userId + routeId + stopId).

**Request body:**
```json
{
  "routeId": 1,
  "stopId": 5,
  "reminderMinutes": 15,
  "enabled": true,
  "dndEnabled": true,
  "dndStart": "22:00",
  "dndEnd": "07:00"
}
```

**Response 200:** the saved preference object.

---

## Workflow Module

Role required: **DISPATCHER**

### POST /api/workflow/tasks
Create a workflow task.

**Request body:**
```json
{
  "type": "ROUTE_CHANGE",
  "title": "Extend route 101 to East District",
  "description": "...",
  "assignedToId": 3,
  "deadline": "2026-04-28T18:00:00",
  "payload": { "affectedStops": [5, 6, 7] }
}
```
Constraints: `type` and `title` not null/blank.

**Response 200:**
```json
{
  "id": 1,
  "taskNumber": "RDC-20260421-1001",
  "type": "ROUTE_CHANGE",
  "title": "Extend route 101 to East District",
  "description": "...",
  "status": "PENDING",
  "assignedToUsername": "bob",
  "assignedToId": 3,
  "deadline": "2026-04-28T18:00:00",
  "escalated": false,
  "payload": { "affectedStops": [5, 6, 7] },
  "createdAt": "2026-04-21T09:00:00",
  "updatedAt": "2026-04-21T09:00:00"
}
```

---

### GET /api/workflow/tasks?page={p}&size={s}&status={status}
List all tasks, optionally filtered by status.

**Query params:** `status` (optional, WorkflowTask.TaskStatus), plus Pageable.

**Response 200:** paginated `WorkflowTaskResponse[]`

---

### GET /api/workflow/tasks/my?page={p}&size={s}
List tasks assigned to the authenticated dispatcher.

**Response 200:** paginated `WorkflowTaskResponse[]`

---

### GET /api/workflow/tasks/{id}
Get a single task by ID.

**Errors:** `404` if not found.

---

### POST /api/workflow/tasks/{id}/approvals
Submit an approval action on a task.

**Request body:**
```json
{ "action": "APPROVE", "comment": "Looks good" }
```
Valid actions: `APPROVE`, `REJECT`, `RETURN`.

**Response 200:**
```json
{
  "id": 1,
  "taskId": 1,
  "approverUsername": "charlie",
  "action": "APPROVE",
  "comment": "Looks good",
  "createdAt": "2026-04-21T10:00:00"
}
```

**Errors:**
- `404` — task not found
- `400` — invalid action

---

### GET /api/workflow/tasks/{id}/approvals
List all approvals for a task.

**Response 200:** `WorkflowApprovalResponse[]`

---

## Admin Module

Role required: **ADMIN**

### GET /api/admin/templates
List all notification templates.

### POST /api/admin/templates
Create or update a template (upsert by name).

**Request body:**
```json
{
  "name": "arrival_reminder",
  "type": "ARRIVAL_REMINDER",
  "titleTemplate": "Arrival Reminder",
  "contentTemplate": "Your bus {routeNumber} departs in {minutes} minutes from {stopName}",
  "sensitivityLevel": 0
}
```

### DELETE /api/admin/templates/{id}
Delete a template.

---

### GET /api/admin/weights
List all sorting weight factors.

### POST /api/admin/weights
Create or update a sorting weight (upsert by factorName).

**Request body:**
```json
{ "factorName": "popularity", "weight": 1.5 }
```
Constraints: `weight` in [0.0, 10.0].

---

### GET /api/admin/dictionaries?fieldName={fieldName}
List field dictionary entries, optionally filtered by `fieldName`.

### POST /api/admin/dictionaries
Create or update a dictionary entry (upsert by fieldName + rawValue).

**Request body:**
```json
{ "fieldName": "stopStatus", "rawValue": "运营", "standardValue": "ACTIVE" }
```

### DELETE /api/admin/dictionaries/{id}
Delete a dictionary entry.

---

### GET /api/admin/configs
List all system config entries.

### POST /api/admin/configs
Create or update a system config entry (upsert by configKey).

**Request body:**
```json
{ "configKey": "max_reservation_per_day", "configValue": "5", "description": "Max reservations per passenger per day" }
```

---

## Bus Data Import Module

Role required: **ADMIN**

### GET /api/bus-data/imports?page={p}&size={s}
List all import jobs, newest first.

**Response 200:** paginated `ImportResponse[]`
```json
{
  "id": 1,
  "importType": "JSON",
  "status": "DONE",
  "fileName": "routes_batch1.json",
  "rowsParsed": 50,
  "rowsFailed": 2,
  "createdAt": "2026-04-21T09:00:00"
}
```

---

### POST /api/bus-data/imports
Upload a bus data file for async import.

**Request:** `multipart/form-data`
- `file`: the file (required)
- `importType`: `JSON` or `HTML` (default: `JSON`)

**Response 200:** `ImportResponse` with `status: PENDING` (processing is async).

---

## Actuator Endpoints

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `GET /actuator/health` | any | Liveness/readiness |
| `GET /actuator/metrics` | any | Micrometer metrics list |
| `GET /actuator/prometheus` | any | Prometheus scrape target |
| `GET /actuator/info` | any | App version info |

---

## Error Response Reference

| HTTP Status | Condition |
|-------------|-----------|
| 400 | Validation failure, business rule violation |
| 401 | Missing or invalid JWT |
| 403 | Insufficient role or ownership violation |
| 404 | Entity not found |
| 409 | Unique constraint conflict (e.g. duplicate username) |
| 500 | Unhandled server error |

All error responses follow the envelope: `{ "success": false, "data": null, "message": "<detail>" }`.
