CREATE TABLE events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);