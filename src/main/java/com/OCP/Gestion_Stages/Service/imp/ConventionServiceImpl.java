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
        if (request.getStatut() != null)
            convention.setStatut(request.getStatut());
        if (request.getDateEmission() != null)
            convention.setDateEmission(request.getDateEmission());
    }

    private ConventionResponse toResponse(Convention c) {
        ConventionResponse response = new ConventionResponse();
        response.setId(c.getId());
        response.setNumero(c.getNumero());
        response.setStatut(c.getStatut());
        response.setDateEmission(c.getDateEmission());
        response.setCreatedAt(c.getCreatedAt());
        if (c.getStage() != null) {
            response.setStageId(c.getStage().getId());
            response.setStageSujet(c.getStage().getSujet());
        }
        return response;
    }
}