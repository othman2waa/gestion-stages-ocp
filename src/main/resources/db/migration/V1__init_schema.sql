-- =============================================
-- TABLE: departement
-- =============================================
CREATE TABLE departement (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    nom         VARCHAR(100) NOT NULL,
    responsable VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: etablissement
-- =============================================
CREATE TABLE etablissement (
    id         BIGSERIAL PRIMARY KEY,
    nom        VARCHAR(150) NOT NULL,
    type       VARCHAR(50),
    ville      VARCHAR(100),
    email      VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: users
-- =============================================
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(30)  NOT NULL,
    actif      BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: stagiaire
-- =============================================
CREATE TABLE stagiaire (
    id               BIGSERIAL PRIMARY KEY,
    nom              VARCHAR(100) NOT NULL,
    prenom           VARCHAR(100) NOT NULL,
    email            VARCHAR(100) NOT NULL UNIQUE,
    telephone        VARCHAR(20),
    cin              VARCHAR(20)  UNIQUE,
    etablissement_id BIGINT REFERENCES etablissement(id),
    filiere          VARCHAR(100),
    niveau           VARCHAR(50),
    user_id          BIGINT REFERENCES users(id),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: encadrant
-- =============================================
CREATE TABLE encadrant (
    id             BIGSERIAL PRIMARY KEY,
    nom            VARCHAR(100) NOT NULL,
    prenom         VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    fonction       VARCHAR(100),
    departement_id BIGINT REFERENCES departement(id),
    user_id        BIGINT REFERENCES users(id),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: stage
-- =============================================
CREATE TABLE stage (
    id             BIGSERIAL    PRIMARY KEY,
    stagiaire_id   BIGINT       NOT NULL REFERENCES stagiaire(id),
    encadrant_id   BIGINT       REFERENCES encadrant(id),
    departement_id BIGINT       REFERENCES departement(id),
    sujet          VARCHAR(255) NOT NULL,
    type_stage     VARCHAR(50)  NOT NULL,
    statut         VARCHAR(50)  NOT NULL DEFAULT 'EN_ATTENTE',
    date_debut     DATE,
    date_fin       DATE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: convention
-- =============================================
CREATE TABLE convention (
    id             BIGSERIAL    PRIMARY KEY,
    stage_id       BIGINT       NOT NULL UNIQUE REFERENCES stage(id),
    numero         VARCHAR(50)  UNIQUE,
    statut         VARCHAR(50)  NOT NULL DEFAULT 'BROUILLON',
    chemin_fichier VARCHAR(255),
    date_emission  DATE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: evaluation
-- =============================================
CREATE TABLE evaluation (
    id             BIGSERIAL PRIMARY KEY,
    stage_id       BIGINT    NOT NULL REFERENCES stage(id),
    encadrant_id   BIGINT    NOT NULL REFERENCES encadrant(id),
    note           DECIMAL(4,2),
    commentaire    TEXT,
    type_eval      VARCHAR(30) NOT NULL,
    date_eval      DATE DEFAULT CURRENT_DATE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TABLE: audit_log
-- =============================================
CREATE TABLE audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    action      VARCHAR(100) NOT NULL,
    utilisateur VARCHAR(100),
    details     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- INDEX pour les performances
-- =============================================
CREATE INDEX idx_stage_stagiaire   ON stage(stagiaire_id);
CREATE INDEX idx_stage_encadrant   ON stage(encadrant_id);
CREATE INDEX idx_stage_statut      ON stage(statut);
CREATE INDEX idx_convention_stage  ON convention(stage_id);
CREATE INDEX idx_evaluation_stage  ON evaluation(stage_id);
```

---

