-- Stockage des fichiers sur disque : chemin relatif + contenu binaire optionnel.
--
-- NB : seule la table document_stagiaire est gérée par Flyway (V16). Les tables
-- candidature et document_candidature sont créées par Hibernate (ddl-auto), donc
-- elles n'existent pas encore au moment où Flyway tourne — on ne les touche PAS ici.
-- Leurs colonnes cv_chemin / chemin_fichier sont ajoutées automatiquement par
-- Hibernate à partir des entités.

ALTER TABLE document_stagiaire ADD COLUMN IF NOT EXISTS chemin_fichier VARCHAR(500);
ALTER TABLE document_stagiaire ALTER COLUMN contenu DROP NOT NULL;
