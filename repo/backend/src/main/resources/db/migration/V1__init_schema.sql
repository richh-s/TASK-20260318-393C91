-- City Bus Platform Schema

CREATE TYPE user_role AS ENUM ('PASSENGER', 'DISPATCHER', 'ADMIN');
CREATE TYPE task_status AS ENUM ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'RETURNED', 'ESCALATED', 'CANCELLED');
CREATE TYPE task_type AS ENUM ('ROUTE_DATA_CHANGE', 'REMINDER_RULE_CONFIG', 'ABNORMAL_DATA_REVIEW');
CREATE TYPE reservation_status AS ENUM ('CONFIRMED', 'CANCELLED', 'MISSED', 'COMPLETED');
CREATE TYPE notification_type AS ENUM ('RESERVATION_SUCCESS', 'ARRIVAL_REMINDER', 'MISSED_CHECKIN', 'TASK_ASSIGNED', 'TASK_ESCALATED', 'SYSTEM');
CREATE TYPE import_status AS ENUM ('PENDING', 'PARSING', 'PARSED', 'FAILED');
CREATE TYPE import_type AS ENUM ('JSON', 'HTML');
CREATE TYPE queue_status AS ENUM ('PENDING', 'PROCESSING', 'DONE', 'FAILED');

-- Users
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        user_role NOT NULL DEFAULT 'PASSENGER',
    display_name VARCHAR(128),
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Bus Routes
CREATE TABLE bus_routes (
    id           BIGSERIAL PRIMARY KEY,
    route_number VARCHAR(32) NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    status       VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Bus Stops
CREATE TABLE bus_stops (
    id               BIGSERIAL PRIMARY KEY,
    route_id         BIGINT NOT NULL REFERENCES bus_routes(id),
    sequence_number  INT NOT NULL,
    name_en          VARCHAR(128) NOT NULL,
    name_cn          VARCHAR(128),
    pinyin           VARCHAR(256),
    pinyin_initials  VARCHAR(64),
    address          VARCHAR(256),
    area_name        VARCHAR(128),
    apartment_type   VARCHAR(64),
    area_sqm         NUMERIC(10,2),
    price_yuan_month NUMERIC(10,2),
    popularity_score INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bus_stops_route ON bus_stops(route_id);
CREATE INDEX idx_bus_stops_name_en ON bus_stops(name_en);
CREATE INDEX idx_bus_stops_pinyin ON bus_stops(pinyin);
CREATE INDEX idx_bus_stops_pinyin_initials ON bus_stops(pinyin_initials);

-- Reservations
CREATE TABLE reservations (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id),
    route_id       BIGINT NOT NULL REFERENCES bus_routes(id),
    stop_id        BIGINT NOT NULL REFERENCES bus_stops(id),
    scheduled_time TIMESTAMP NOT NULL,
    status         reservation_status NOT NULL DEFAULT 'CONFIRMED',
    notes          TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_user ON reservations(user_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_scheduled ON reservations(scheduled_time);

-- Notification Preferences
CREATE TABLE notification_preferences (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id),
    route_id         BIGINT REFERENCES bus_routes(id),
    stop_id          BIGINT REFERENCES bus_stops(id),
    reminder_minutes INT NOT NULL DEFAULT 10,
    dnd_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    dnd_start        TIME,
    dnd_end          TIME,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, route_id, stop_id)
);

-- Notifications
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    type       notification_type NOT NULL,
    title      VARCHAR(256) NOT NULL,
    content    TEXT NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    entity_id  BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(user_id, is_read);

-- Notification Templates
CREATE TABLE notification_templates (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(128) NOT NULL UNIQUE,
    type             notification_type NOT NULL,
    title_template   VARCHAR(256) NOT NULL,
    content_template TEXT NOT NULL,
    sensitivity_level INT NOT NULL DEFAULT 0,
    created_by       BIGINT NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Sorting Weights
CREATE TABLE sorting_weights (
    id           BIGSERIAL PRIMARY KEY,
    factor_name  VARCHAR(64) NOT NULL UNIQUE,
    weight       NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    description  TEXT,
    updated_by   BIGINT REFERENCES users(id),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Field Dictionaries
CREATE TABLE field_dictionaries (
    id             BIGSERIAL PRIMARY KEY,
    field_name     VARCHAR(64) NOT NULL,
    raw_value      VARCHAR(256) NOT NULL,
    standard_value VARCHAR(256) NOT NULL,
    created_by     BIGINT NOT NULL REFERENCES users(id),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(field_name, raw_value)
);

-- System Config
CREATE TABLE system_configs (
    id          BIGSERIAL PRIMARY KEY,
    config_key  VARCHAR(64) NOT NULL UNIQUE,
    config_value VARCHAR(512) NOT NULL,
    description TEXT,
    updated_by  BIGINT REFERENCES users(id),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Workflow Tasks
CREATE TABLE workflow_tasks (
    id           BIGSERIAL PRIMARY KEY,
    task_number  VARCHAR(32) NOT NULL UNIQUE,
    type         task_type NOT NULL,
    title        VARCHAR(256) NOT NULL,
    description  TEXT,
    status       task_status NOT NULL DEFAULT 'PENDING',
    created_by   BIGINT NOT NULL REFERENCES users(id),
    assigned_to  BIGINT REFERENCES users(id),
    deadline     TIMESTAMP,
    entity_id    BIGINT,
    entity_type  VARCHAR(64),
    payload      JSONB,
    escalated    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_tasks_status ON workflow_tasks(status);
CREATE INDEX idx_workflow_tasks_assigned ON workflow_tasks(assigned_to);
CREATE INDEX idx_workflow_tasks_created ON workflow_tasks(created_by);

-- Workflow Approvals
CREATE TABLE workflow_approvals (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT NOT NULL REFERENCES workflow_tasks(id),
    approver_id BIGINT NOT NULL REFERENCES users(id),
    action     VARCHAR(32) NOT NULL,
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_approvals_task ON workflow_approvals(task_id);

-- Bus Data Imports
CREATE TABLE bus_data_imports (
    id            BIGSERIAL PRIMARY KEY,
    filename      VARCHAR(256) NOT NULL,
    import_type   import_type NOT NULL,
    version       INT NOT NULL DEFAULT 1,
    status        import_status NOT NULL DEFAULT 'PENDING',
    rows_parsed   INT DEFAULT 0,
    rows_failed   INT DEFAULT 0,
    error_message TEXT,
    created_by    BIGINT NOT NULL REFERENCES users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMP
);

-- Bus Data Import Rows (parsed records)
CREATE TABLE bus_data_import_rows (
    id             BIGSERIAL PRIMARY KEY,
    import_id      BIGINT NOT NULL REFERENCES bus_data_imports(id),
    row_index      INT NOT NULL,
    raw_data       JSONB,
    mapped_data    JSONB,
    status         VARCHAR(16) NOT NULL DEFAULT 'OK',
    error_message  TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_import_rows_import ON bus_data_import_rows(import_id);

-- Message Queue
CREATE TABLE message_queue (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(64) NOT NULL,
    payload     JSONB NOT NULL,
    status      queue_status NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error       TEXT,
    scheduled_at TIMESTAMP,
    processed_at TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_message_queue_status ON message_queue(status, scheduled_at);

-- Audit Logs
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id   BIGINT,
    user_id     BIGINT REFERENCES users(id),
    ip_address  VARCHAR(45),
    trace_id    VARCHAR(64),
    details     JSONB,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- Cleaning Rules
CREATE TABLE cleaning_rules (
    id           BIGSERIAL PRIMARY KEY,
    field_name   VARCHAR(64) NOT NULL UNIQUE,
    rule_type    VARCHAR(32) NOT NULL,
    rule_config  JSONB,
    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_by   BIGINT NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
