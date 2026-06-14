CREATE TABLE suivis_hebdomadaires (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
    encadrant_id BIGINT NOT NULL REFERENCES users(id),
    semaine_numero INT NOT NULL,
    date_suivi DATE NOT NULL,
    progression INT DEFAULT 0 CHECK (progression >= 0 AND progression <= 100),
    commentaire TEXT,
    points_positifs TEXT,
    axes_amelioration TEXT,
    note DECIMAL(4,2) CHECK (note >= 0 AND note <= 20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);