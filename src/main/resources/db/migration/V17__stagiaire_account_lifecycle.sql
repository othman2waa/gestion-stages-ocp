-- Date prévue de désactivation du compte stagiaire (30 jours après TERMINE)
ALTER TABLE stagiaire ADD COLUMN IF NOT EXISTS date_desactivation_prevue DATE;

-- Log de désactivation des comptes
CREATE TABLE compte_stagiaire_log (
    id              BIGSERIAL PRIMARY KEY,
    stagiaire_id    BIGINT NOT NULL REFERENCES stagiaire(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    stage_id        BIGINT REFERENCES stage(id),
    action          VARCHAR(50) NOT NULL,
    date_fin_stage  DATE,
    date_action     TIMESTAMP DEFAULT NOW(),
    details         TEXT
);

CREATE INDEX idx_compte_log_stagiaire ON compte_stagiaire_log(stagiaire_id);

COMMENT ON TABLE compte_stagiaire_log IS 'Historique de désactivation/réactivation des comptes stagiaires';
COMMENT ON COLUMN compte_stagiaire_log.action IS 'DESACTIVATION_AUTO, DESACTIVATION_MANUELLE, REACTIVATION';
