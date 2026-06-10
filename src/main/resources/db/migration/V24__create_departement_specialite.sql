-- Spécialités rattachées à un département
CREATE TABLE IF NOT EXISTS departement_specialite (
    id BIGSERIAL PRIMARY KEY,
    departement_id BIGINT NOT NULL REFERENCES departement(id) ON DELETE CASCADE,
    nom VARCHAR(100) NOT NULL
);
