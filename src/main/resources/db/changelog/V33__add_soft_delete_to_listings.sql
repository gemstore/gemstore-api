ALTER TABLE listings ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;
ALTER TABLE listings ADD COLUMN deleted_by BIGINT REFERENCES users_table(id) ON DELETE SET NULL;

-- Partial index — only indexes non-deleted rows, keeps it small and fast
CREATE INDEX idx_listings_deleted_at ON listings(deleted_at) WHERE deleted_at IS NULL;