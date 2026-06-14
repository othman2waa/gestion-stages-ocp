# Plan de rapport de PFE — Plateforme de Gestion Digitalisée des Stages (OCP Group)

> Guide de rédaction et support de soutenance. Plan ancré sur le code réel : noms de classes, tables et technologies effectivement présents dans le projet (Backend Spring Boot 4 / Frontend Angular 18 / PostgreSQL).

---

## INTRODUCTION GÉNÉRALE
- Contexte du PFE : digitalisation d'un processus RH critique au sein d'un grand groupe industriel.
- Annonce du fil conducteur : concevoir et réaliser une application full-stack sécurisée couvrant **tout le cycle de vie d'un stage**, de la candidature publique à l'archivage, en intégrant des briques d'**IA locale** (Ollama).
- Présentation de la structure du rapport (5 chapitres).

---

## CHAPITRE 1 — CONTEXTE GÉNÉRAL, PROBLÉMATIQUE & ANALYSE DES BESOINS

### 1.1 Organisme d'accueil et enjeux métier
- **OCP Group** : leader mondial du phosphate et dérivés ; site de **Benguérir** (écosystème UM6P / Mohammed VI Green City) comme cadre du stage.
- Enjeux de la **digitalisation des processus internes RH** : centralisation, traçabilité, réduction des délais, conformité documentaire, pilotage par indicateurs.
- État des lieux du processus « stage » avant le projet : dispersion (mails, fichiers, présentiel), absence de suivi d'état, ressaisies, contrôle documentaire manuel.

### 1.2 Problématique et objectifs
- **Problématique** : comment outiller de bout en bout la gestion des stages — candidature, sélection, conventionnement, suivi, évaluation, archivage — tout en garantissant **sécurité**, **traçabilité** et **intégrité des données** ?
- **Objectifs** :
  - Portail public de **candidature** + offres de stage (sans authentification).
  - Espaces différenciés par rôle (RH, encadrant, stagiaire).
  - **Machine à états** du dossier de stage entièrement outillée.
  - **Aide à la décision par IA locale** (matching CV/besoin, vérification documentaire, assistant RH).
  - **Pilotage** : tableaux de bord, indicateurs, exports.
- **Valeur ajoutée attendue** : gain de temps RH, fiabilisation du dossier, conformité, visibilité temps réel.

### 1.3 Méthodologie (approche agile)
- Démarche itérative et incrémentale (lots fonctionnels successifs : candidatures → conventionnement → suivi → rapport → IA → reporting).
- Backlog organisé par **acteur** et par **axe de flux** (cf. branche `feature/axe2-flux-ameliore`).
- Livraisons fréquentes et validation continue des API.

### 1.4 Acteurs du système
Quatre rôles applicatifs (enum **`UserRole`**) :
- **`ADMIN_RH`** : administration complète, comptes, validation finale, reporting, archivage.
- **`RESPONSABLE_RH`** : pilotage et reporting.
- **`ENCADRANT`** : sélection des candidats, suivi, appréciations, validation du rapport.
- **`STAGIAIRE`** : candidature, dépôt de documents, fiche de renseignement, rapport.

### 1.5 Besoins fonctionnels majeurs
- **Gestion des utilisateurs et des comptes** : `users`, `stagiaire`, `encadrant`, cycle de vie du compte stagiaire (activation/désactivation, `compte_stagiaire_log`).
- **Candidature publique** : `candidature` + `document_candidature`, dépôt CV, **scoring IA** (`score_matching`).
- **Flux de transition des états** du stage (enum **`StageStatus`**) : `EN_ATTENTE → DEMANDE_SOUMISE → EN_ATTENTE_VALIDATION → VALIDEE → CONVENTION_GENEREE → CONVENTION_SIGNEE → EN_COURS → EN_ATTENTE_EVALUATION → TERMINE` (+ `REJETEE`, `ANNULE`).
- **Conventionnement** : génération de convention (`convention`, enum `ConventionStatus`).
- **Suivi** : `suivis_hebdomadaires`, `pointages`, sujets (`sujet_stage`).
- **Évaluation** : `evaluation`, fiches d'appréciation (`fiche_appreciation_stage`, `fiche_appreciation_stagiaire`).
- **Rapport de stage** : dépôt stagiaire → **validation encadrant** (`rapport_stage.statut` ∈ {EN_ATTENTE, VALIDE, REFUSE}) → archivage dans les documents (`document_stagiaire` type `RAPPORT`).
- **Attestation** (`attestation_stage`), **archivage** (`archive_stage`), **onboarding** (`onboarding_checklist`).
- **Notifications** (`notification`) et **traçabilité** (`audit_log`, `stage_historique`).

### 1.6 Besoins non-fonctionnels
- **Sécurité** : authentification **JWT stateless**, autorisation par rôle (`@PreAuthorize`, `SecurityConfig`).
- **Performance** : pagination serveur (`Pageable`), requêtes ciblées, **stockage fichiers sur disque** (évite de charger les `byte[]` en mémoire), interceptor de **cache** côté front.
- **Intégrité des données** : contraintes relationnelles (clés étrangères, unicité), transactions (`@Transactional`), validation (`spring-boot-starter-validation`).
- **Traçabilité / auditabilité**, **maintenabilité** (architecture en couches), **documentation API** (Swagger / springdoc-openapi).

### 1.7 Cas d'utilisation clés (description textuelle)
- **Postuler à un stage** (visiteur) : formulaire public → création `candidature` + upload CV → scoring IA → notification RH/encadrant.
- **Sélectionner un candidat** (encadrant) : consultation classée, décision, planification d'entretien, génération de convocation avec période de stage.
- **Valider un dossier** (RH) : passage `VALIDEE → CONVENTION_GENEREE`, ouverture de l'espace complet du stagiaire.
- **Déposer & valider un rapport** (stagiaire → encadrant) : upload → statut EN_ATTENTE → valider/refuser + commentaire → archivage.
- **Piloter l'activité** (RH) : tableau de bord, indicateurs, **assistant RH IA**, exports Excel/PDF.

---

## CHAPITRE 2 — ARCHITECTURE TECHNIQUE & CONCEPTION

### 2.1 Vue d'ensemble et choix d'architecture full-stack
- **Découplage** strict : API REST **Spring Boot** (serveur) ⇄ **SPA Angular** (client), communication JSON sur HTTP, sécurité par jeton.
- Justification : séparation des responsabilités, équipes/déploiements indépendants, testabilité, réutilisabilité de l'API (web, mobile futur).
- **Monolithe modulaire** (et non microservices) : adapté à la taille du projet et au délai, tout en gardant des frontières de modules nettes (préparant un découpage ultérieur).
- Briques transverses : **IA locale Ollama** (HTTP), **OCR Tesseract** (Tess4J), **PDFBox**, **e-mail** (SMTP), **Swagger**.

### 2.2 Modélisation de la base de données (PostgreSQL)
- **SGBD** : PostgreSQL ; **ORM** : Spring Data JPA / Hibernate 7.
- **26 entités** (package `domain.model`) ; 5 énumérations (`StageStatus`, `TypeStage`, `UserRole`, `ConventionStatus`, `TypeEvaluation`).
- **Table pivot `stage`** : relie `stagiaire`, `encadrant`, `departement` ; porte `statut`, `type_stage`, `date_debut/fin`. Autour gravitent : `convention` (1–1), `evaluation` (1–N), `rapport_stage` (1–1), `fiche_appreciation_*`, `suivis_hebdomadaires`, `pointages`, `attestation_stage`, `stage_historique`, `archive_stage`.
- **Relations clés** :
  - `users` 1–1 `stagiaire` / `encadrant` (un compte = un profil métier).
  - `etablissement` 1–N `stagiaire` ; `departement` 1–N `encadrant`/`stage`/`departement_specialite`/`sujet_stage`/`candidature`.
  - `candidature` 1–N `document_candidature` ; `annonce_stage` 1–N `candidature`.
  - `stagiaire` 1–N `document_stagiaire`/`onboarding_checklist`.
- **Importance de la modélisation relationnelle** :
  - **Clés étrangères** garantissant l'intégrité référentielle (ex. `document_candidature.candidature_id … ON DELETE CASCADE`).
  - **Contraintes d'unicité** (ex. `document_stagiaire (stagiaire_id, type_document)` — un seul document par type ; `archive_stage.stage_id` unique).
  - **Cycle de vie des entités** matérialisé par `StageStatus` et journalisé (`stage_historique`, `audit_log`).
- Le **diagramme entité-association** est fourni (`docs/database-erd.mmd` Mermaid, `docs/database-erd.puml` PlantUML).

### 2.3 Stratégie de versioning et de gestion du schéma
- Outils : **Flyway** (migrations `db/migration/V1…V27`) + **Hibernate `ddl-auto: validate`** (vérification entités ↔ schéma au démarrage).
- **Justification de Flyway** : versionner le schéma, rejouer un environnement de façon reproductible, historiser les évolutions, éviter les dérives manuelles.
- **Choix de stockage des fichiers** : `FileStorageService` (chemin disque, colonne `chemin_fichier`) avec rétro-compatibilité `bytea` (`contenu`) — discussion performance/portabilité.
- **Honnêteté technique (point de discussion en soutenance)** : le schéma actuel a été matérialisé par Hibernate ; les migrations ont été **consolidées (V20–V27)** pour couvrir l'intégralité des entités, et l'application tourne en `validate`. Perspective : câblage complet de Flyway sous Spring Boot 4 pour un provisioning 100 % par migrations.

### 2.4 Conception des API et conventions
- **API RESTful** : ressources nommées (`/api/stages`, `/api/candidatures`, `/api/rapports/...`), verbes HTTP, codes de statut, pagination/tri.
- **DTO** (package `domain.dto`) : découplage entité ↔ représentation exposée (ex. `StagiaireResponse`, `RapportResponse`) ; protection des données et stabilité du contrat.
- Documentation interactive via **springdoc-openapi (Swagger UI)**.

---

## CHAPITRE 3 — RÉALISATION & IMPLÉMENTATION BACKEND (Java / Spring Boot)

### 3.1 Organisation du code (architecture en couches)
- Packages : **`Controller`** (23 contrôleurs REST) → **`Service.interfaces` / `Service.imp`** (logique métier, interface + implémentation) → **`Repository`** (Spring Data JPA) → **`domain.model`** (entités) / **`domain.dto`** (contrats) / **`domain.enums`**.
- Transverses : **`config`** (sécurité, JWT, CORS, Swagger, seed), **`exeptions`** (gestion d'erreurs).
- Justification : **séparation des responsabilités**, inversion de dépendances (interfaces de service), testabilité unitaire, lisibilité.

### 3.2 Persistance avec Spring Data JPA / Hibernate
- **Repositories** dérivés de `JpaRepository` ; requêtes dérivées (`findByStagiaireId`, `findByStageId`…) et **requêtes natives paginées** (ex. `StagiaireRepository.rechercher(... etatStage ..., Pageable)` avec clauses `EXISTS`).
- Gestion des **relations** : `@ManyToOne(fetch = LAZY)` côté enfants, `@OneToMany` avec `cascade` ciblé (ex. `candidature.documents`), `@Enumerated(EnumType.STRING)` pour les statuts.
- **Optimisation des requêtes** : pagination/tri (`Pageable`, `Sort`), projections/DTO, chargement paresseux, séparation des `byte[]` (fichiers sur disque) pour ne pas saturer la mémoire.
- **Transactions** : `@Transactional` au niveau service (cohérence des opérations multi-étapes : ex. validation de rapport + archivage).

### 3.3 Sécurité applicative (Spring Security + JWT)
- **`SecurityConfig`** : API **stateless** (`SessionCreationPolicy.STATELESS`), `csrf` désactivé (API token), règles d'accès par `requestMatchers` :
  - Public : `/api/auth/**`, `POST /api/candidatures`, `GET /api/departements/actifs`, `/api/annonces/publiques`, Swagger.
  - Restreint : `/api/admin/**` → `ADMIN_RH` ; `/api/reporting/**` → `ADMIN_RH`/`RESPONSABLE_RH` ; le reste authentifié.
- **Chaîne JWT** : `AuthController` (login) → `JwtService` (génération/validation via **jjwt**, clé et expiration externalisées) → **`JwtAuthenticationFilter`** (placé avant `UsernamePasswordAuthenticationFilter`, extrait/valide le token, peuple le `SecurityContext`).
- **Autorisation fine** : `@PreAuthorize("hasAnyRole(...)")` sur les endpoints sensibles (ex. validation de rapport réservée à `ENCADRANT`/`ADMIN_RH`/`RESPONSABLE_RH`).
- **Bootstrap** : `DataSeeder` (compte `admin.rh` initial), `ApplicationConfig` (beans d'auth), `CorsConfig`.
- Le jeton est stocké côté client dans `localStorage` (objet `currentUser`).

### 3.4 Logique métier remarquable
- **Machine à états du stage** : transitions contrôlées dans les services (ex. décision encadrant, génération de convention idempotente, ouverture de l'espace stagiaire à `CONVENTION_GENEREE`).
- **Workflow rapport** : `RapportStageServiceImpl` (upload, `valider(decision, commentaire)`, archivage vers `document_stagiaire`, e-mail au stagiaire).
- **Intelligence artificielle locale (Ollama)** :
  - `OllamaService` : extraction d'infos CV, **scoring de matching** candidat/besoin, vérification documentaire, assistant RH (RAG simple sur indicateurs).
  - `ChatbotService` : assistant encadrant (classement des candidats) et assistant RH (réponses sur les indicateurs, ex. liste des fiches non remplies).
  - `DocumentVerificationService` : **OCR (Tess4J)** + extraction PDF (**PDFBox**) → vérification d'authenticité/conformité, en asynchrone (`@Async`).
- **Notifications & e-mails** : `spring-boot-starter-mail` (convocations, validations, réinitialisations).

### 3.5 Gestion globale des exceptions
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) : centralise la traduction des erreurs en réponses HTTP homogènes (`ErrorResponse`).
- Exceptions métier dédiées : `BusinessException`, `ResourceNotFoundException`, `UnauthorizedException` → codes 4xx explicites, messages clairs côté client.

---

## CHAPITRE 4 — RÉALISATION & IMPLÉMENTATION FRONTEND (Angular)

### 4.1 Structure de l'application Angular
- **Angular 18** (`@angular/core ^18`) + **Angular Material** (`^18.2`).
- Architecture **hybride** : modules à chargement différé (`dashboard`, `stagiaires`, `stages`, `conventions`, `evaluations`, `encadrants`, `auth`, `stagiaire-portal`) **et ~49 composants `standalone`** (portail de candidature, dashboards, chatbots…).
- **Lazy loading** par route (`app.routes.ts`) : portail public (`/postuler`, `/offres`, `/candidature`), espaces protégés.
- **Couche `core/services`** (~28 services) : un service par domaine (`stage.service`, `candidature.service`, `rapport.service`, `stagiaire.service`, `export.service`, `auth.service`…).

### 4.2 Sécurité des routes et intercepteurs
- **`authGuard`** : protège les routes authentifiées (redirection login).
- **Intercepteurs HTTP** :
  - `auth.interceptor` : injection automatique du **Bearer JWT** (depuis `localStorage`).
  - `error.interceptor` : gestion centralisée des erreurs HTTP (401/expiration, messages).
  - `cache.interceptor` : mise en cache de requêtes (performance).

### 4.3 Programmation réactive avec RxJS
- **Observables** issus de `HttpClient` ; `subscribe` dans les composants, `pipe` d'opérateurs.
- Cas concrets : **recherche avec `debounceTime` + `distinctUntilChanged`** (listes), `takeUntilDestroyed` (désabonnement), composition de flux (chargement parallèle des « extras » d'un stage : rapport + fiches).
- Gestion des `@Input` asynchrones via `ngOnChanges` (convention projet).

### 4.4 Design, UX/UI et restitution
- **Identité OCP** : charte verte/orange, page **« careers »** publique (`/postuler`) avec hero, présentation société, activités, galerie ; **logo OCP** intégré dans tous les espaces (sidebar RH/encadrant, navbar stagiaire).
- **Ergonomie** : layout flex (sidebar repliable + contenu), états vides/erreurs, confirmations, badges de statut lisibles (pipe `StatutLabelPipe`), séparation **actifs / terminés** des stagiaires.
- **Visualisation & exports** : graphiques **Chart.js / ng2-charts** (tableaux de bord), exports **Excel (`xlsx`)** et **PDF (`jspdf` + `jspdf-autotable`)** via `ExportService`, impression.
- **Assistants IA** intégrés à l'UI (chatbot encadrant, assistant RH).

---

## CHAPITRE 5 — QUALITÉ LOGICIELLE, TESTS, DÉPLOIEMENT & CONCLUSION

### 5.1 Stratégie de tests
- **Tests unitaires (JUnit 5 + Mockito)** sur la couche service : `AnnonceServiceImplTest`, `CandidatureServiceImplTest`, `DepartementServiceImplTest`, `ConventionServiceImplTest`, `DocumentStagiaireServiceImplTest` (mock des repositories, vérification de la logique métier).
- **Tests d'intégration** : `RepositoryIntegrationTest` sur **PostgreSQL réel via Testcontainers** (validation du mapping et des requêtes), `FileStorageServiceTest` (stockage disque), `GestionStagesApplicationTests` (montée du contexte).
- **Couverture** : plugin **JaCoCo** (rapport de couverture).
- **Validation des API** : Swagger UI (springdoc) pour l'exploration/essai manuel des endpoints.

### 5.2 Bonnes pratiques
- **Clean Code** : nommage explicite, interfaces de service, DTO, responsabilités séparées.
- **API RESTful** : ressources, verbes, codes HTTP, pagination.
- **Gestion globale des exceptions** : `@RestControllerAdvice` (`GlobalExceptionHandler`) + exceptions métier.
- **Sécurité par conception** : stateless, moindre privilège (`@PreAuthorize`), secrets externalisés.
- **Traçabilité** : `audit_log`, `stage_historique`.

### 5.3 Conteneurisation & déploiement
- **Docker** : `Dockerfile` backend (image Java), `Dockerfile` frontend + **`nginx.conf`** (build Angular servi par Nginx), orchestration **`docker-compose.yml`** (backend + frontend + PostgreSQL).
- Discussion : variables d'environnement (datasource, `ddl-auto`, JWT, Ollama), volumes (uploads, données Postgres), réseau interne.
- Pistes d'optimisation : build multi-étapes, healthchecks, profils Spring.

### 5.4 Bilan technique et compétences acquises
- Conception et réalisation d'une **application full-stack sécurisée** de bout en bout.
- Maîtrise de **Spring Boot / Spring Security / JPA**, d'**Angular / RxJS**, de **PostgreSQL**, de **Docker**.
- Intégration d'**IA locale** (LLM + OCR) dans un cadre métier réel.
- Modélisation de données, machine à états, qualité logicielle (tests, couverture).

### 5.5 Perspectives d'évolution
- **CI/CD** (build/test/déploiement automatisés).
- **Câblage complet de Flyway** (provisioning 100 % par migrations) sous Spring Boot 4.
- **Messagerie asynchrone (RabbitMQ)** pour notifications/traitements IA découplés.
- Évolution vers une **architecture microservices** (extraction des modules candidatures, suivi, IA).
- Migration **complète du stockage fichiers sur disque/objet** (suppression du `bytea` legacy), observabilité (logs/metrics).

---

## CONCLUSION GÉNÉRALE
- Rappel de la problématique et synthèse des apports (digitalisation complète, sécurité, IA d'aide à la décision, pilotage).
- Ouverture sur l'industrialisation (CI/CD, scalabilité) et l'extension fonctionnelle.

---

### Annexes suggérées
- Diagramme entité-association (`docs/database-erd.*`).
- Diagrammes de cas d'utilisation et de séquence (login JWT, dépôt+validation rapport).
- Captures d'écran (portail candidature, dashboards, espaces par rôle).
- Extraits de code commentés (filtre JWT, `SecurityConfig`, workflow rapport).
