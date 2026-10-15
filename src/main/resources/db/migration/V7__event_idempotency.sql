ALTER TABLE events ADD COLUMN idempotency_key VARCHAR(128);

CREATE UNIQUE INDEX events_owner_idempotency_key_idx
    ON events(owner_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
