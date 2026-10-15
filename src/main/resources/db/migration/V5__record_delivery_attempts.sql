ALTER TABLE deliveries DROP CONSTRAINT deliveries_status_check;
ALTER TABLE deliveries ADD CONSTRAINT deliveries_status_check
    CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'DEAD'));
ALTER TABLE deliveries ALTER COLUMN next_attempt_at DROP NOT NULL;
ALTER TABLE deliveries ADD COLUMN last_attempt_at TIMESTAMPTZ;
ALTER TABLE deliveries ADD COLUMN delivered_at TIMESTAMPTZ;
ALTER TABLE deliveries ADD COLUMN last_http_status INTEGER;

CREATE TABLE delivery_attempts (
    id UUID PRIMARY KEY,
    delivery_id UUID NOT NULL REFERENCES deliveries(id),
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    http_status INTEGER CHECK (http_status BETWEEN 100 AND 599),
    duration_ms BIGINT NOT NULL CHECK (duration_ms >= 0),
    error_category VARCHAR(32),
    attempted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT delivery_attempt_number_unique UNIQUE (delivery_id, attempt_number)
);

CREATE INDEX delivery_attempts_delivery_idx ON delivery_attempts(delivery_id, attempt_number);
