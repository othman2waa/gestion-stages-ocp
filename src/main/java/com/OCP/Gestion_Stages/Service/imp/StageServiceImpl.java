package com.OCP.Gestion_Stages.Service.imp;



import com.OCP.Gestion_Stages.domain.dto.stage.StageRequest;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EncadrantRepository encadrantRepository;
    private final DepartementRepository departementRepository;

    @Override
    public StageResponse createStage(StageRequest request) {
        Stagiaire stagiaire = stagiaireRepository.findById(request.getStagiaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", request.getStagiaireId()));

        Encadrant encadrant = null;
        if (request.getEncadrantId() != null)
            encadrant = encadrantRepository.findById(request.getEncadrantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Encadrant", request.getEncadrantId()));

        Departement departement = null;
        if (request.getDepartementId() != null)
            departement = departementRepository.findById(request.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departement", request.getDepartementId()));

        Stage stage = Stage.builder()
                .stagiaire(stagiaire)
                .encadrant(encadrant)
                .departement(departement)
                .sujet(request.getSujet())
                .typeStage(request.getTypeStage())
                .statut(StageStatus.EN_ATTENTE)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .build();

        return toResponse(stageRepository.save(stage));
    }

    @Override
    @Transactional(readOnly = true)
    public StageResponse getStageById(Long id) {
        return toResponse(stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageResponse> getAllStages() {
        return stageRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageResponse> getStagesByStagiaire(Long stagiaireId) {
        return stageRepository.findByStagiaireId(stagiaireId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageResponse> getStagesByEncadrant(Long encadrantId) {
        return stageRepository.findByEncadrantId(encadrantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageResponse> getStagesByStatut(StageStatus statut) {
        return stageRepository.findByStatut(statut).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StageResponse updateStage(Long id, StageRequest request) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage", id));

        stage.setSujet(request.getSujet());
        stage.setTypeStage(request.getTypeStage());
        stage.setDateDebut(request.getDateDebut());
        stage.setDateFin(request.getDateFin());

        return toResponse(stageRepository.save(stage));
    }

    @Override
    public StageResponse updateStatut(Long id, StageStatus statut) {
        Stage stage = stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage", id));
        stage.setStatut(statut);
        return toResponse(stageRepository.save(stage));
    }

    @Override
    public void deleteStage(Long id) {
        if (!stageRepository.existsById(id))
            throw new ResourceNotFoundException("Stage", id);
        stageRepository.deleteById(id);
    }

    private StageResponse toResponse(Stage s) {
        return StageResponse.builder()
                .id(s.getId())
                .stagiaireNom(s.getStagiaire().getNom())
                .stagiairePrenom(s.getStagiaire().getPrenom())
                .encadrantNom(s.getEncadrant() != null ?
                        s.getEncadrant().getNom() + " " + s.getEncadrant().getPrenom() : null)
                .departementNom(s.getDepartement() != null ? s.getDepartement().getNom() : null)
                .sujet(s.getSujet())
                .typeStage(s.getTypeStage())
                .statut(s.getStatut())
                .dateDebut(s.getDateDebut())
                .dateFin(s.getDateFin())
                .createdAt(s.getCreatedAt())
                .build();
    }
}

