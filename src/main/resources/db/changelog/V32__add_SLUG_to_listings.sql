ALTER TABLE listings ADD COLUMN slug VARCHAR(12) UNIQUE;

-- Backfill existing listings with a slug
UPDATE listings
SET slug = LOWER(SUBSTRING(MD5(RANDOM()::TEXT || id::TEXT), 1, 11))
WHERE slug IS NULL;

ALTER TABLE listings ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX idx_listings_slug ON listings(slug);