-- Documents joints à une candidature
CREATE TABLE IF NOT EXISTS document_candidature (
    id BIGSERIAL PRIMARY KEY,
    candidature_id BIGINT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    type_document VARCHAR(50) NOT NULL,
    nom_fichier VARCHAR(255),
    contenu BYTEA,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut_ia VARCHAR(30) DEFAULT 'NON_VERIFIE',
    score_ia INTEGER,
    commentaire_ia TEXT,
    chemin_fichier VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_document_candidature ON document_candidature(candidature_id);
