package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.StageService;
import com.OCP.Gestion_Stages.domain.dto.stage.StageRequest;
import com.OCP.Gestion_Stages.domain.dto.stage.StageResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
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
        stage.setStatut(statut);
        return toResponse(stageRepository.save(stage));
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