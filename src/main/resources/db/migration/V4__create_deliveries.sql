-- Composite keys keep a delivery within the same account as its event and destination.
CREATE UNIQUE INDEX events_id_owner_id_idx ON events(id, owner_id);
CREATE UNIQUE INDEX webhook_endpoints_id_owner_id_idx ON webhook_endpoints(id, owner_id);

CREATE TABLE deliveries (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES accounts(id),
    event_id UUID NOT NULL,
    endpoint_id UUID NOT NULL,
    endpoint_url VARCHAR(2048) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT deliveries_event_owner_fk FOREIGN KEY (event_id, owner_id)
        REFERENCES events(id, owner_id),
    CONSTRAINT deliveries_endpoint_owner_fk FOREIGN KEY (endpoint_id, owner_id)
        REFERENCES webhook_endpoints(id, owner_id),
    CONSTRAINT deliveries_event_endpoint_unique UNIQUE (event_id, endpoint_id)
);

CREATE INDEX deliveries_owner_created_idx ON deliveries(owner_id, created_at DESC);
CREATE INDEX deliveries_owner_event_idx ON deliveries(owner_id, event_id);
CREATE INDEX deliveries_pending_idx ON deliveries(next_attempt_at) WHERE status = 'PENDING';
