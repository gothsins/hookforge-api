CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE access_tokens (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX access_tokens_account_id_idx ON access_tokens(account_id);

-- Existing local rows are preserved without an owner and remain inaccessible through the API.
ALTER TABLE webhook_endpoints ADD COLUMN owner_id UUID REFERENCES accounts(id);
ALTER TABLE events ADD COLUMN owner_id UUID REFERENCES accounts(id);
ALTER TABLE webhook_endpoints ADD CONSTRAINT webhook_endpoints_owner_required
    CHECK (owner_id IS NOT NULL) NOT VALID;
ALTER TABLE events ADD CONSTRAINT events_owner_required
    CHECK (owner_id IS NOT NULL) NOT VALID;
CREATE INDEX webhook_endpoints_owner_id_idx ON webhook_endpoints(owner_id);
CREATE INDEX events_owner_id_idx ON events(owner_id);
