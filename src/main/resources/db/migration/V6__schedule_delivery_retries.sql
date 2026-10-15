UPDATE deliveries SET next_attempt_at = updated_at
WHERE status = 'FAILED' AND next_attempt_at IS NULL;

DROP INDEX deliveries_pending_idx;
CREATE INDEX deliveries_due_idx ON deliveries(next_attempt_at)
WHERE status IN ('PENDING', 'FAILED');
