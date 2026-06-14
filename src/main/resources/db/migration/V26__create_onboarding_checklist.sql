-- Checklist d'intégration (onboarding) du stagiaire
CREATE TABLE IF NOT EXISTS onboarding_checklist (
    id BIGSERIAL PRIMARY KEY,
    stagiaire_id BIGINT REFERENCES stagiaire(id) ON DELETE CASCADE,
    etape VARCHAR(255) NOT NULL,
    categorie VARCHAR(255) NOT NULL,
    description TEXT,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    completed_by VARCHAR(255),
    ordre INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
