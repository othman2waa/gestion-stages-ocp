-- Demandes d'attestation de stage
CREATE TABLE IF NOT EXISTS attestation_stage (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT REFERENCES stage(id),
    statut VARCHAR(255) DEFAULT 'EN_ATTENTE',
    date_demande TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_traitement TIMESTAMP,
    traite_par VARCHAR(255),
    commentaire TEXT,
    numero_attestation VARCHAR(255)
);
