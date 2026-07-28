CREATE TABLE IF NOT EXISTS security_alerts (
    id                     BIGSERIAL PRIMARY KEY,
    category               VARCHAR(50)  NOT NULL,
    severity               VARCHAR(20)  NOT NULL,
    origin_system          VARCHAR(20)  NOT NULL,
    source_type            VARCHAR(30)  NOT NULL,
    subject_student_id     VARCHAR(100),
    subject_name           VARCHAR(255),
    reported_by_user_id    VARCHAR(100),
    reported_by_name       VARCHAR(255),
    description            TEXT,
    latitude               DOUBLE PRECISION,
    longitude              DOUBLE PRECISION,
    status                 VARCHAR(20)  NOT NULL,
    acknowledged_by_name   VARCHAR(255),
    acknowledged_at        TIMESTAMP,
    resolved_by_name       VARCHAR(255),
    resolved_at            TIMESTAMP,
    resolution_notes       TEXT,
    external_alert_id      VARCHAR(100),
    external_system        VARCHAR(20),
    occurred_at            TIMESTAMP    NOT NULL,
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_security_alerts_status ON security_alerts (status);
CREATE INDEX IF NOT EXISTS idx_security_alerts_category ON security_alerts (category);
CREATE INDEX IF NOT EXISTS idx_security_alerts_external_alert_id ON security_alerts (external_alert_id);
