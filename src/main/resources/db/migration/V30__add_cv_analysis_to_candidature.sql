-- Précalcul IA du CV à l'ingestion (dépôt de candidature) : texte extrait + embedding vectoriel.
-- Permet à la recherche sémantique de l'encadrant d'être quasi instantanée (le traitement lourd
-- est fait une seule fois, en tâche de fond, et non à chaque requête).
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS cv_texte     TEXT;
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS cv_embedding TEXT;
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS cv_traite    BOOLEAN DEFAULT FALSE;
