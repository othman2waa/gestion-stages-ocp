-- Enrichir le profil stagiaire
ALTER TABLE stagiaire ADD COLUMN IF NOT EXISTS date_naissance DATE;
ALTER TABLE stagiaire ADD COLUMN IF NOT EXISTS sexe VARCHAR(10);
ALTER TABLE stagiaire ADD COLUMN IF NOT EXISTS nationalite VARCHAR(50);
ALTER TABLE stagiaire ADD COLUMN IF NOT EXISTS lieu_naissance VARCHAR(100);

-- Table de documents personnels du stagiaire
CREATE TABLE document_stagiaire (
    id              BIGSERIAL PRIMARY KEY,
    stagiaire_id    BIGINT NOT NULL REFERENCES stagiaire(id) ON DELETE CASCADE,
    type_document   VARCHAR(50) NOT NULL,
    nom_fichier     VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100),
    taille          BIGINT,
    contenu         BYTEA NOT NULL,
    uploaded_at     TIMESTAMP DEFAULT NOW(),
    UNIQUE(stagiaire_id, type_document)
);

CREATE INDEX idx_doc_stagiaire_id ON document_stagiaire(stagiaire_id);

COMMENT ON TABLE document_stagiaire IS 'Documents personnels du stagiaire (CV, CIN, photo, diplome, etc.)';
COMMENT ON COLUMN document_stagiaire.type_document IS 'CV, CIN, PHOTO, DIPLOME, ASSURANCE, AUTRE';
