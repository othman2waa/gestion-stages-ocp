package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.ArchiveStageService;
import com.OCP.Gestion_Stages.Service.AttestationPdfService;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.interfaces.StageService;
import com.OCP.Gestion_Stages.domain.model.AttestationStage;
import com.OCP.Gestion_Stages.domain.dto.stage.StageRequest;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.OCP.Gestion_Stages.domain.enums.TypeStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;



@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EncadrantRepository encadrantRepository;
    private final DepartementRepository departementRepository;
    private final ArchiveStageService archiveStageService;
    private final AttestationStageRepository attestationRepository;
    private final AttestationPdfService attestationPdfService;
    private final EmailService emailService;



    @Override
    public Page<StageResponse> rechercher(String keyword, StageStatus statut, String typeStage, Long departementId, Long encadrantId, Pageable pageable) {
        String statutStr = statut != null ? statut.name() : null;
        String typeStr = (typeStage != null && !typeStage.isEmpty()) ? typeStage : null;
        return stageRepository.rechercher(keyword, statutStr, typeStr, departementId, encadrantId, pageable)
                .map(this::toResponse);
    }
    @Override
    public List<StageResponse> findAll() {
        return stageRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public StageResponse findById(Long id) {
        return toResponse(stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable : " + id)));
    }

    @Override
    public StageResponse create(StageRequest request) {
        Stage stage = new Stage();
        mapToEntity(request, stage);
        return toResponse(stageRepository.save(stage));
    }

    @Override
    public StageResponse update(Long id, StageRequest request) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable : " + id));
        mapToEntity(request, stage);
        return toResponse(stageRepository.save(stage));
    }

    @Override
    public void delete(Long id) {
        if (!stageRepository.existsById(id))
            throw new ResourceNotFoundException("Stage introuvable : " + id);
        stageRepository.deleteById(id);
    }

    @Override
    public StageResponse updateStatut(Long id, StageStatus statut) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable : " + id));
        // Cohérence du cycle de vie : interdit les transitions illégitimes
        stage.getStatut().assertCanTransitionTo(statut);
        stage.setStatut(statut);
        Stage saved = stageRepository.save(stage);

        // ── Trigger automatique attestation + archivage
        if (statut == StageStatus.TERMINE) {
            // Auto-générer attestation si elle n'existe pas encore
            try {
                if (attestationRepository.findByStageId(id).isEmpty()) {
                    AttestationStage att = new AttestationStage();
                    att.setStage(saved);
                    att.setStatut("APPROUVEE");
                    att.setDateTraitement(java.time.LocalDateTime.now());
                    att.setTraitePar("SYSTEME_AUTO");
                    att.setNumeroAttestation("ATT-" + id + "-" +
                            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")));
                    attestationRepository.save(att);
                    log.info("Attestation auto-générée pour stage {}", id);

                    // Générer PDF et envoyer par email
                    if (saved.getStagiaire() != null && saved.getStagiaire().getEmail() != null) {
                        try {
                            byte[] pdf = attestationPdfService.genererAttestation(att);
                            String nomStagiaire = saved.getStagiaire().getPrenom() + " " + saved.getStagiaire().getNom();
                            emailService.envoyerAttestation(saved.getStagiaire().getEmail(), nomStagiaire, pdf);
                        } catch (Exception ex) {
                            log.warn("Erreur envoi attestation par email stage {}: {}", id, ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Erreur génération attestation auto stage {}: {}", id, e.getMessage());
            }

            // Archivage automatique
            try {
                archiveStageService.archiverStage(saved, "SYSTEME_AUTO");
                log.info("Stage {} archivé automatiquement", id);
            } catch (Exception e) {
                log.warn("Erreur archivage automatique stage {}: {}", id, e.getMessage());
            }
        }

        return toResponse(saved);
    }
    @Override
    public List<StageResponse> findByStagiaire(Long stagiaireId) {
        return stageRepository.findByStagiaireId(stagiaireId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<StageResponse> findByEncadrant(Long encadrantId) {
        return stageRepository.findByEncadrantId(encadrantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void mapToEntity(StageRequest request, Stage stage) {
        stage.setSujet(request.getSujet());
        stage.setTypeStage(request.getTypeStage());
        stage.setDateDebut(request.getDateDebut());
        stage.setDateFin(request.getDateFin());
        if (request.getStatut() != null) stage.setStatut(request.getStatut());

        Stagiaire stagiaire = stagiaireRepository.findById(request.getStagiaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));
        stage.setStagiaire(stagiaire);

        if (request.getEncadrantId() != null) {
            Encadrant encadrant = encadrantRepository.findById(request.getEncadrantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
            stage.setEncadrant(encadrant);
        }
        if (request.getDepartementId() != null) {
            Departement departement = departementRepository.findById(request.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departement introuvable"));
            stage.setDepartement(departement);
        }
    }

    private StageResponse toResponse(Stage s) {
        StageResponse response = new StageResponse();
        response.setId(s.getId());
        response.setSujet(s.getSujet());
        response.setTypeStage(s.getTypeStage());
        response.setStatut(s.getStatut());
        response.setDateDebut(s.getDateDebut());
        response.setDateFin(s.getDateFin());
        response.setCreatedAt(s.getCreatedAt());
        if (s.getStagiaire() != null) {
            response.setStagiaireId(s.getStagiaire().getId());
            response.setStagiaireNom(s.getStagiaire().getNom() + " " + s.getStagiaire().getPrenom());
        }
        if (s.getEncadrant() != null) {
            response.setEncadrantId(s.getEncadrant().getId());
            response.setEncadrantNom(s.getEncadrant().getNom() + " " + s.getEncadrant().getPrenom());
        }
        if (s.getDepartement() != null) {
            response.setDepartementId(s.getDepartement().getId());
            response.setDepartementNom(s.getDepartement().getNom());
        }
        return response;
    }
}