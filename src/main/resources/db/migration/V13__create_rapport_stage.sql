CREATE TABLE rapport_stage (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
    nom_fichier VARCHAR(255) NOT NULL,
    type_contenu VARCHAR(100) NOT NULL,
    taille BIGINT,
    contenu BYTEA NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);