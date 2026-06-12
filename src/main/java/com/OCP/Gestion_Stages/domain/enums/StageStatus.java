package com.OCP.Gestion_Stages.domain.enums;

public enum StageStatus {
    EN_ATTENTE,
    DEMANDE_SOUMISE,
    EN_ATTENTE_VALIDATION,
    VALIDEE,
    REJETEE,
    CONVENTION_GENEREE,
    CONVENTION_SIGNEE,
    EN_COURS,
    EN_ATTENTE_EVALUATION,
    TERMINE,
    ANNULE;

    public boolean canTransitionTo(StageStatus next) {
        if (next == this) return true; // idempotent
        return switch (this) {
            case EN_ATTENTE, DEMANDE_SOUMISE -> next == EN_ATTENTE_VALIDATION || next == ANNULE;
            case EN_ATTENTE_VALIDATION -> next == VALIDEE || next == REJETEE;
            case VALIDEE -> next == CONVENTION_GENEREE || next == ANNULE;
            case CONVENTION_GENEREE -> next == CONVENTION_SIGNEE || next == ANNULE;
            case CONVENTION_SIGNEE -> next == EN_COURS || next == ANNULE;
            case EN_COURS -> next == EN_ATTENTE_EVALUATION || next == ANNULE;
            case EN_ATTENTE_EVALUATION -> next == TERMINE;
            default -> false;
        };
    }

    /** Libellé lisible pour les messages d'erreur métier. */
    public String libelle() {
        return name().replace('_', ' ').toLowerCase();
    }

    /**
     * Vérifie et impose une transition d'état ; lève une {@link com.OCP.Gestion_Stages.exeptions.BusinessException}
     * si la transition est illégitime. Centralise la cohérence du cycle de vie du stage.
     */
    public void assertCanTransitionTo(StageStatus next) {
        if (!canTransitionTo(next)) {
            throw new com.OCP.Gestion_Stages.exeptions.BusinessException(
                    "Transition de stage invalide : « " + libelle() + " » ne peut pas passer à « "
                            + next.libelle() + " ».");
        }
    }
}