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

@Service
@RequiredArgsConstructor
@Transactional
public class ConventionServiceImpl implements ConventionService {

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
}