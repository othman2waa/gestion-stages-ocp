package com.OCP.Gestion_Stages.Repository;

import com.OCP.Gestion_Stages.domain.model.Candidature;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration sur un VRAI PostgreSQL (Testcontainers) — pas de H2,
 * donc compatibilité totale avec les types Postgres (bytea) et le schéma réel.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Repositories — intégration PostgreSQL (Testcontainers)")
class RepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private CandidatureRepository candidatureRepository;
    @Autowired private DocumentStagiaireRepository documentStagiaireRepository;
    @Autowired private StagiaireRepository stagiaireRepository;

    @Test
    @DisplayName("findCvIdsAMigrer ne renvoie que les CV encore en base (bytea) sans chemin disque")
    void findCvIdsAMigrer_filtre_legacy() {
        Candidature legacy = new Candidature();
        legacy.setNom("Legacy"); legacy.setPrenom("Cv"); legacy.setEmail("legacy@test.com");
        legacy.setCvContenu("pdf-bytes".getBytes()); // stocké en base, pas de chemin

        Candidature migree = new Candidature();
        migree.setNom("Migree"); migree.setPrenom("Cv"); migree.setEmail("migree@test.com");
        migree.setCvChemin("uuid_cv.pdf"); // déjà sur disque

        candidatureRepository.saveAll(List.of(legacy, migree));

        List<Long> aMigrer = candidatureRepository.findCvIdsAMigrer();

        assertThat(aMigrer).contains(legacy.getId()).doesNotContain(migree.getId());
    }

    @Test
    @DisplayName("findMetaByStagiaireId renvoie les métadonnées sans charger le contenu binaire")
    void findMetaByStagiaireId_projection() {
        Stagiaire s = new Stagiaire();
        s.setNom("Test"); s.setPrenom("Stagiaire"); s.setEmail("stg@test.com");
        s = stagiaireRepository.save(s);

        documentStagiaireRepository.save(DocumentStagiaire.builder()
                .stagiaire(s).typeDocument("CV").nomFichier("cv.pdf")
                .contentType("application/pdf").taille(123L)
                .cheminFichier("uuid_cv.pdf")
                .build());

        List<Object[]> meta = documentStagiaireRepository.findMetaByStagiaireId(s.getId());

        assertThat(meta).hasSize(1);
        assertThat(meta.get(0)[1]).isEqualTo("CV");        // typeDocument
        assertThat(meta.get(0)[2]).isEqualTo("cv.pdf");    // nomFichier
    }
}
