-- ===================================================================
-- UNZA Clinic Database Schema - Version 4
-- Created: 2026-05-12
-- Description: Login audit trail for security monitoring
-- ===================================================================

CREATE TABLE IF NOT EXISTS login_audit_log (
    id BIGSERIAL PRIMARY KEY,
    audit_id VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100),
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason VARCHAR(200),
    user_id VARCHAR(50),
    role VARCHAR(50),
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lal_email ON login_audit_log(email);
CREATE INDEX idx_lal_ip ON login_audit_log(ip_address);
CREATE INDEX idx_lal_success ON login_audit_log(success);
CREATE INDEX idx_lal_logged_at ON login_audit_log(logged_at);

COMMIT;
