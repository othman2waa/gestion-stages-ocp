package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import com.OCP.Gestion_Stages.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AttestationPdfService — génération des PDF (smoke tests)")
class AttestationPdfServiceTest {

    private final AttestationPdfService service = new AttestationPdfService();

    private AttestationStage attestationComplete(String statut) {
        Etablissement etab = new Etablissement();
        etab.setNom("Université Mohammed VI Polytechnique");

        Stagiaire st = new Stagiaire();
        st.setNom("Kozal"); st.setPrenom("Amal"); st.setEmail("amal@example.com");
        st.setCin("AB123456"); st.setFiliere("Génie informatique"); st.setNiveau("Bac+5");
        st.setEtablissement(etab);

        Departement dept = new Departement();
        dept.setNom("Transformation digitale"); dept.setCode("DT");

        Encadrant enc = new Encadrant();
        enc.setNom("El Amrani"); enc.setPrenom("Karim");

        Stage stage = new Stage();
        stage.setId(7L); stage.setSujet("Plateforme de gestion des stages");
        stage.setStagiaire(st); stage.setEncadrant(enc); stage.setDepartement(dept);
        stage.setTypeStage(TypeStage.PFE);
        stage.setDateDebut(LocalDate.of(2026, 1, 1));
        stage.setDateFin(LocalDate.of(2026, 5, 1));
        stage.setEntiteAccueil("Direction Capital Humain — Benguérir");

        AttestationStage att = new AttestationStage();
        att.setStage(stage); att.setStatut(statut);
        att.setNumeroAttestation("ATT-7-202605");
        att.setDateTraitement(LocalDateTime.now());
        att.setTraitePar("rh.admin");
        return att;
    }

    @Test
    @DisplayName("genererAttestation produit un PDF non vide")
    void genererAttestation() {
        byte[] pdf = service.genererAttestation(attestationComplete("APPROUVEE"));
        assertThat(pdf).isNotNull().isNotEmpty();
        // En-tête de fichier PDF : %PDF
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("genererDemande produit un PDF non vide")
    void genererDemande() {
        byte[] pdf = service.genererDemande(attestationComplete("EN_ATTENTE"));
        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
