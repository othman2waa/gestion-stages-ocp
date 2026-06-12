-- Rétention des archives : flags d'anonymisation (purge des données personnelles après expiration)
ALTER TABLE archive_stage ADD COLUMN IF NOT EXISTS anonymise BOOLEAN;
ALTER TABLE archive_stage ADD COLUMN IF NOT EXISTS date_anonymisation TIMESTAMP;
