CREATE TABLE pointages (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
    date_pointage DATE NOT NULL,
    present BOOLEAN NOT NULL DEFAULT TRUE,
    motif TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (stage_id, date_pointage)
);
