# Base de données — architecture & gestion

## Stack
- **PostgreSQL** (`gestion_stages_db`)
- **Spring Data JPA / Hibernate 7** (ORM, dialecte PostgreSQL)
- **26 entités**, 5 enums (`StageStatus`, `TypeStage`, `UserRole`, `ConventionStatus`, `TypeEvaluation`)
- **Flyway** déclaré (`flyway-database-postgresql` 11) — voir note ci-dessous

## Gestion du schéma
La **source de vérité du schéma est le code des entités JPA**.

| Paramètre | Valeur | Rôle |
|-----------|--------|------|
| `spring.jpa.hibernate.ddl-auto` | `validate` | Au démarrage, Hibernate vérifie que les entités correspondent au schéma. L'app ne démarre pas en cas d'écart. |

- En **développement**, on a temporairement utilisé `ddl-auto=update` (auto-création/complétion des tables). Le schéma actuel a entièrement été construit ainsi.
- On est **revenu à `validate`** : le schéma existant correspond aux entités, l'app démarre normalement sans modifier la base.

### Note sur Flyway
Sous **Spring Boot 4**, l'auto-configuration Flyway n'est pas active dans ce projet (module d'auto-config non tiré) : **Flyway n'a jamais exécuté de migration** (pas de table `flyway_schema_history`). Les fichiers `db/migration/` documentent néanmoins le schéma et sont désormais **complets** :

- `V1` … `V19` : tables historiques
- `V20` … `V27` : tables auparavant créées par Hibernate (annonce_stage, candidature, document_candidature, sujet_stage, departement_specialite, attestation_stage, onboarding_checklist, archive_stage) — désormais versionnées (idempotentes via `CREATE TABLE IF NOT EXISTS`).

**Pour réactiver Flyway proprement (post-soutenance, sur base neuve)** :
1. Ajouter le starter/auto-config Flyway adapté à Spring Boot 4.
2. Provisionner une base vierge : Flyway joue `V1`→`V27`.
3. Garder `ddl-auto=validate`.

## Diagramme entité-association
- `database-erd.mmd` — **Mermaid** (rendu direct sur GitHub, https://mermaid.live, ou dans le rapport).
- `database-erd.puml` — **PlantUML** (rendu sur http://www.plantuml.com/plantuml ou extension IDE ; nécessite Graphviz en local).

Table pivot : **`stage`** (relie stagiaire, encadrant, département) ; autour gravitent convention, évaluations, rapport, fiches d'appréciation, suivis, pointages, attestation, archive.
