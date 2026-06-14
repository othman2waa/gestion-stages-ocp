-- Archivage des stages terminés (dénormalisé pour historique)
CREATE TABLE IF NOT EXISTS archive_stage (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT UNIQUE,
    stagiaire_nom VARCHAR(255),
    stagiaire_prenom VARCHAR(255),
    stagiaire_email VARCHAR(255),
    stagiaire_filiere VARCHAR(255),
    stagiaire_niveau VARCHAR(255),
    stagiaire_etablissement VARCHAR(255),
    encadrant_nom VARCHAR(255),
    encadrant_prenom VARCHAR(255),
    encadrant_email VARCHAR(255),
    departement_nom VARCHAR(255),
    sujet TEXT,
    type_stage VARCHAR(255),
    date_debut DATE,
    date_fin DATE,
    note_finale NUMERIC(4,2),
    mention VARCHAR(255),
    annee_stage INTEGER,
    date_archivage TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    archive_par VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_archive_annee ON archive_stage(annee_stage);
CREATE INDEX IF NOT EXISTS idx_archive_dept ON archive_stage(departement_nom);
