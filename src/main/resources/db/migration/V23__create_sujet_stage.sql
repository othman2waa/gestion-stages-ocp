-- Sujets de stage (proposés par encadrants / RH)
CREATE TABLE IF NOT EXISTS sujet_stage (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    technologies VARCHAR(255),
    niveau_requis VARCHAR(255),
    type_stage VARCHAR(255),
    encadrant_id BIGINT REFERENCES encadrant(id),
    departement_id BIGINT REFERENCES departement(id),
    statut VARCHAR(255) DEFAULT 'PROPOSE',
    stage_id BIGINT REFERENCES stage(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    validated_at TIMESTAMP,
    validated_by VARCHAR(255)
);
