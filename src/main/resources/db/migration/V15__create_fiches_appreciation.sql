-- Fiche d'appréciation de stage (remplie par l'encadrant)
CREATE TABLE fiche_appreciation_stage (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES stage(id),
    encadrant_id BIGINT NOT NULL REFERENCES encadrant(id),
    note_globale INT CHECK (note_globale BETWEEN 1 AND 5),
    qualite_travail INT CHECK (qualite_travail BETWEEN 1 AND 5),
    respect_delais INT CHECK (respect_delais BETWEEN 1 AND 5),
    initiative INT CHECK (initiative BETWEEN 1 AND 5),
    qualite_rapport INT CHECK (qualite_rapport BETWEEN 1 AND 5),
    competences_techniques INT CHECK (competences_techniques BETWEEN 1 AND 5),
    recommandation VARCHAR(50),
    commentaires TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(stage_id)
);

-- Fiche d'appréciation qualité stagiaire (remplie par l'encadrant)
CREATE TABLE fiche_appreciation_stagiaire (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES stage(id),
    encadrant_id BIGINT NOT NULL REFERENCES encadrant(id),
    assiduite INT CHECK (assiduite BETWEEN 1 AND 5),
    ponctualite INT CHECK (ponctualite BETWEEN 1 AND 5),
    comportement_professionnel INT CHECK (comportement_professionnel BETWEEN 1 AND 5),
    esprit_equipe INT CHECK (esprit_equipe BETWEEN 1 AND 5),
    communication INT CHECK (communication BETWEEN 1 AND 5),
    adaptation INT CHECK (adaptation BETWEEN 1 AND 5),
    recommandation_embauche VARCHAR(50),
    commentaires TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(stage_id)
);
