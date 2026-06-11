-- Entité / service d'accueil affecté au stage (figure sur la convocation)
ALTER TABLE stage ADD COLUMN IF NOT EXISTS entite_accueil VARCHAR(255);
