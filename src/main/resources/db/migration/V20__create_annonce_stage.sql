-- Offres de stage publiques (créé par Hibernate jusqu'ici, désormais versionné)
CREATE TABLE IF NOT EXISTS annonce_stage (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    competences_requises TEXT,
    departement VARCHAR(255),
    type_stage VARCHAR(255),
    niveau_requis VARCHAR(255),
    filiere_requise VARCHAR(255),
    nombre_postes INTEGER DEFAULT 1,
    date_limite DATE,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_par VARCHAR(255)
);
