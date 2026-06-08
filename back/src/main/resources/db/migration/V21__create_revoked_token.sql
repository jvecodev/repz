-- V20 foi intencionalmente pulada. A próxima migration deve ser V24__.

CREATE TABLE IF NOT EXISTS revoked_token (
    jti        VARCHAR(255) PRIMARY KEY,
    expires_at TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_revoked_token_expires_at ON revoked_token (expires_at);
