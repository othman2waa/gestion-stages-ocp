package com.OCP.Gestion_Stages.domain.enums;

import com.OCP.Gestion_Stages.exeptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StageStatus — machine à états du dossier de stage")
class StageStatusTest {

    @Test
    @DisplayName("le chemin nominal complet est autorisé")
    void cheminNominalAutorise() {
        assertThat(StageStatus.EN_ATTENTE.canTransitionTo(StageStatus.EN_ATTENTE_VALIDATION)).isTrue();
        assertThat(StageStatus.EN_ATTENTE_VALIDATION.canTransitionTo(StageStatus.VALIDEE)).isTrue();
        assertThat(StageStatus.VALIDEE.canTransitionTo(StageStatus.CONVENTION_GENEREE)).isTrue();
        assertThat(StageStatus.CONVENTION_GENEREE.canTransitionTo(StageStatus.CONVENTION_SIGNEE)).isTrue();
        assertThat(StageStatus.CONVENTION_SIGNEE.canTransitionTo(StageStatus.EN_COURS)).isTrue();
        assertThat(StageStatus.EN_COURS.canTransitionTo(StageStatus.EN_ATTENTE_EVALUATION)).isTrue();
        assertThat(StageStatus.EN_ATTENTE_EVALUATION.canTransitionTo(StageStatus.TERMINE)).isTrue();
    }

    @Test
    @DisplayName("l'annulation est possible depuis plusieurs états intermédiaires")
    void annulationPossible() {
        assertThat(StageStatus.VALIDEE.canTransitionTo(StageStatus.ANNULE)).isTrue();
        assertThat(StageStatus.CONVENTION_SIGNEE.canTransitionTo(StageStatus.ANNULE)).isTrue();
        assertThat(StageStatus.EN_COURS.canTransitionTo(StageStatus.ANNULE)).isTrue();
    }

    @Test
    @DisplayName("le rejet de validation est autorisé")
    void rejetAutorise() {
        assertThat(StageStatus.EN_ATTENTE_VALIDATION.canTransitionTo(StageStatus.REJETEE)).isTrue();
    }

    @Test
    @DisplayName("les transitions illégitimes sont interdites")
    void transitionsIllegitimes() {
        assertThat(StageStatus.EN_ATTENTE.canTransitionTo(StageStatus.TERMINE)).isFalse();
        assertThat(StageStatus.TERMINE.canTransitionTo(StageStatus.EN_COURS)).isFalse();
        assertThat(StageStatus.EN_COURS.canTransitionTo(StageStatus.VALIDEE)).isFalse();
        assertThat(StageStatus.REJETEE.canTransitionTo(StageStatus.VALIDEE)).isFalse();
    }

    @Test
    @DisplayName("rester dans le même état est idempotent")
    void memeEtatIdempotent() {
        assertThat(StageStatus.EN_COURS.canTransitionTo(StageStatus.EN_COURS)).isTrue();
    }

    @Test
    @DisplayName("assertCanTransitionTo lève une BusinessException si la transition est invalide")
    void assertLeveSiInvalide() {
        assertThatThrownBy(() -> StageStatus.EN_ATTENTE.assertCanTransitionTo(StageStatus.TERMINE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    @DisplayName("assertCanTransitionTo passe sans erreur si la transition est valide")
    void assertPasseSiValide() {
        assertThatCode(() -> StageStatus.EN_COURS.assertCanTransitionTo(StageStatus.EN_ATTENTE_EVALUATION))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("le libellé est lisible (minuscules, sans underscore)")
    void libelleLisible() {
        assertThat(StageStatus.EN_ATTENTE_VALIDATION.libelle()).isEqualTo("en attente validation");
    }
}
