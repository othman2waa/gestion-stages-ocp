package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.ConventionRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionService;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionRequest;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
import com.OCP.Gestion_Stages.domain.model.Convention;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import java.util.List;
import java.util.stream.Collectors;
import com.OCP.Gestion_Stages.domain.dto.convention.ConventionDTO;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionServiceExtended;

@Service
@RequiredArgsConstructor
@Transactional
public class ConventionServiceImpl implements ConventionService, ConventionServiceExtended {

    private final ConventionRepository conventionRepository;
    private final StageRepository stageRepository;

    @Override
    public List<ConventionResponse> findAll() {
        return conventionRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ConventionResponse findById(Long id) {
        return toResponse(conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable : " + id)));
    }

    @Override
    public ConventionResponse create(ConventionRequest request) {
        Convention convention = new Convention();
        mapToEntity(request, convention);
        return toResponse(conventionRepository.save(convention));
    }

    @Override
    public ConventionResponse update(Long id, ConventionRequest request) {
        Convention convention = conventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable : " + id));
        mapToEntity(request, convention);
        return toResponse(conventionRepository.save(convention));
    }

    @Override
    public void delete(Long id) {
        if (!conventionRepository.existsById(id))
            throw new ResourceNotFoundException("Convention introuvable : " + id);
        conventionRepository.deleteById(id);
    }

    @Override
    public ConventionResponse findByStage(Long stageId) {
        return toResponse(conventionRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Convention introuvable pour stage : " + stageId)));
    }

    private void mapToEntity(ConventionRequest request, Convention convention) {
        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        convention.setStage(stage);
        convention.setNumero(request.getNumero());

        if (request.getStatut() != null) {
            convention.setStatut(request.getStatut());
            // Changement statut automatique
            mettreAJourStatutStage(stage, request.getStatut());
        }
        if (request.getDateEmission() != null)
            convention.setDateEmission(request.getDateEmission());
    }

    private void mettreAJourStatutStage(Stage stage, ConventionStatus statut) {
        switch (statut) {
            case EN_VALIDATION -> stage.setStatut(StageStatus.CONVENTION_GENEREE);
            case SIGNEE -> {
                if (stage.getDateDebut() != null && !stage.getDateDebut().isAfter(java.time.LocalDate.now())) {
                    stage.setStatut(StageStatus.EN_COURS);
                } else {
                    stage.setStatut(StageStatus.CONVENTION_SIGNEE);
                }
            }
            case ARCHIVEE -> stage.setStatut(StageStatus.TERMINE);
            default -> {}
        }
        stageRepository.save(stage);
    }

    private ConventionResponse toResponse(Convention c) {
        ConventionResponse response = new ConventionResponse();
        response.setId(c.getId());
        response.setNumero(c.getNumero());
        response.setStatut(c.getStatut());
        response.setDateEmission(c.getDateEmission());
        response.setCreatedAt(c.getCreatedAt());
        if (c.getStage() != null) {
            Stage s = c.getStage();
            response.setStageId(s.getId());
            response.setStageSujet(s.getSujet());
            response.setStageDebut(s.getDateDebut());
            response.setStageFin(s.getDateFin());
            response.setTypeStage(s.getTypeStage() != null ? s.getTypeStage().name() : "");
            if (s.getStagiaire() != null) {
                response.setStagiaireNom(s.getStagiaire().getPrenom() + " " + s.getStagiaire().getNom());
                response.setStagiaireEmail(s.getStagiaire().getEmail());
                response.setStagiaireCin(s.getStagiaire().getCin());
                response.setStagiaireFiliere(s.getStagiaire().getFiliere());
                response.setStagiaireNiveau(s.getStagiaire().getNiveau());
                if (s.getStagiaire().getEtablissement() != null)
                    response.setStagiaireEtablissement(s.getStagiaire().getEtablissement().getNom());
            }
            if (s.getEncadrant() != null) {
                response.setEncadrantNom(s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom());
                response.setEncadrantEmail(s.getEncadrant().getEmail());
            }
            if (s.getDepartement() != null)
                response.setDepartementNom(s.getDepartement().getNom());
        }
        return response;
    }



// Implémentation ConventionServiceExtended


    @Override
    public List<ConventionDTO> getAllDTO() {
        return conventionRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public ConventionDTO getByIdDTO(Long id) {
        return toDTO(conventionRepository.findById(id)
                .orElseThrow(() -> new com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException("Convention introuvable")));
    }

    @Override
    public ConventionDTO generer(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException("Stage introuvable"));
        // Idempotent : si une convention existe déjà pour ce stage, on la renvoie
        // (évite les doublons et ne régresse pas le statut du stage).
        var existante = conventionRepository.findByStageId(stageId);
        if (existante.isPresent()) return toDTO(existante.get());

        // Filet : si le stage n'a pas de dates, on en pose par défaut (sinon convocation sans période)
        if (stage.getDateDebut() == null) stage.setDateDebut(java.time.LocalDate.now());
        if (stage.getDateFin() == null)   stage.setDateFin(stage.getDateDebut().plusMonths(4));

        Convention c = Convention.builder()
                .stage(stage)
                .statut(com.OCP.Gestion_Stages.domain.enums.ConventionStatus.EN_VALIDATION)
                .dateEmission(java.time.LocalDate.now())
                .numero("CONV-" + stageId + "-" + java.time.LocalDate.now().getYear())
                .build();
        stage.setStatut(com.OCP.Gestion_Stages.domain.enums.StageStatus.CONVENTION_GENEREE);
        stageRepository.save(stage);
        return toDTO(conventionRepository.save(c));
    }

    @Override
    public ConventionDTO signerManuel(Long id) {
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException("Convention introuvable"));
        c.setStatut(com.OCP.Gestion_Stages.domain.enums.ConventionStatus.SIGNEE);
        c.setStatutSignature("SIGNEE_COMPLET");
        Stage stage = c.getStage();
        stage.setStatut(com.OCP.Gestion_Stages.domain.enums.StageStatus.CONVENTION_SIGNEE);
        stageRepository.save(stage);
        return toDTO(conventionRepository.save(c));
    }

    @Override
    public java.util.Map<String, Object> signerElectronique(Long id, String signature, String cible, String username) {
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException("Convention introuvable"));

        String base64 = signature.replace("data:image/png;base64,", "");
        byte[] signatureBytes = java.util.Base64.getDecoder().decode(base64);

        if ("stagiaire".equals(cible)) {
            c.setSignatureStagiaire(signatureBytes);
            c.setDateSignatureStagiaire(java.time.LocalDateTime.now());
        } else {
            c.setSignatureEncadrant(signatureBytes);
            c.setDateSignatureEncadrant(java.time.LocalDateTime.now());
        }

        boolean stagiaireSigne = c.getSignatureStagiaire() != null;
        boolean encadrantSigne = c.getSignatureEncadrant() != null;

        if (stagiaireSigne && encadrantSigne) {
            c.setStatutSignature("SIGNEE_COMPLET");
            c.setStatut(com.OCP.Gestion_Stages.domain.enums.ConventionStatus.SIGNEE);
            Stage stage = c.getStage();
            stage.setStatut(com.OCP.Gestion_Stages.domain.enums.StageStatus.CONVENTION_SIGNEE);
            stageRepository.save(stage);
        } else {
            c.setStatutSignature("PARTIELLEMENT_SIGNEE");
        }

        conventionRepository.save(c);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("message", "Signature enregistree");
        result.put("statutSignature", c.getStatutSignature());
        result.put("stagiaireSigne", stagiaireSigne);
        result.put("encadrantSigne", encadrantSigne);
        return result;
    }

    @Override
    public java.util.Map<String, Object> getSignatureStatus(Long id) {
        Convention c = conventionRepository.findById(id)
                .orElseThrow(() -> new com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException("Convention introuvable"));
        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("statutSignature", c.getStatutSignature() != null ? c.getStatutSignature() : "EN_ATTENTE");
        status.put("stagiaireSigne", c.getSignatureStagiaire() != null);
        status.put("encadrantSigne", c.getSignatureEncadrant() != null);
        status.put("dateSignatureStagiaire", c.getDateSignatureStagiaire() != null ? c.getDateSignatureStagiaire().toString() : null);
        status.put("dateSignatureEncadrant", c.getDateSignatureEncadrant() != null ? c.getDateSignatureEncadrant().toString() : null);
        return status;
    }

    @Override
    public ConventionDTO toDTO(Convention c) {
        Stage s = c.getStage();
        return ConventionDTO.builder()
                .id(c.getId())
                .numero(c.getNumero())
                .statut(c.getStatut() != null ? c.getStatut().name() : "")
                .statutSignature(c.getStatutSignature() != null ? c.getStatutSignature() : "EN_ATTENTE")
                .stagiaireSigne(c.getSignatureStagiaire() != null)
                .encadrantSigne(c.getSignatureEncadrant() != null)
                .dateSignatureStagiaire(c.getDateSignatureStagiaire())
                .dateSignatureEncadrant(c.getDateSignatureEncadrant())
                .dateEmission(c.getDateEmission())
                .createdAt(c.getCreatedAt())
                .stageId(s != null ? s.getId() : null)
                .stagiaireNom(s != null && s.getStagiaire() != null ?
                        s.getStagiaire().getPrenom() + " " + s.getStagiaire().getNom() : "")
                .encadrantNom(s != null && s.getEncadrant() != null ?
                        s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom() : "")
                .sujet(s != null ? s.getSujet() : "")
                .dateDebut(s != null && s.getDateDebut() != null ? s.getDateDebut().toString() : "")
                .dateFin(s != null && s.getDateFin() != null ? s.getDateFin().toString() : "")
                .build();
    }
}