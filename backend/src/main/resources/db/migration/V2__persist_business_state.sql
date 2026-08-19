CREATE TABLE conversation (
  id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(120) NOT NULL,
  active_intent VARCHAR(40), state VARCHAR(40) NOT NULL, slots_json TEXT NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_conversation_user ON conversation (user_id, updated_at);

CREATE TABLE idempotency_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR(120) NOT NULL,
  request_key VARCHAR(64) NOT NULL, response_json TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, request_key)
);
CREATE INDEX idx_idempotency_expiry ON idempotency_record (expires_at);

CREATE TABLE pending_action (
  token VARCHAR(64) PRIMARY KEY, conversation_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(120) NOT NULL, type VARCHAR(32) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL, consumed BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_action_user ON pending_action (user_id);
CREATE INDEX idx_action_expiry ON pending_action (expires_at);

CREATE TABLE business_record (
  id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(120) NOT NULL,
  conversation_id VARCHAR(64) NOT NULL, type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL, amount DECIMAL(12,2), description VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_business_user_time ON business_record (user_id, created_at);
