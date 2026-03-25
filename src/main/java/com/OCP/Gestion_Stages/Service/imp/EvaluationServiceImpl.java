package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.EvaluationService;
import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationRequest;
import com.OCP.Gestion_Stages.domain.dto.evaluation.EvaluationResponse;
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
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final StageRepository stageRepository;
    private final EncadrantRepository encadrantRepository;
    private final UserRepository userRepository;
    private final StagiaireRepository stagiaireRepository;
    @Override
    public List<EvaluationResponse> findAll() {
        return evaluationRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EvaluationResponse findById(Long id) {
        return toResponse(evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation introuvable : " + id)));
    }

    @Override
    public EvaluationResponse create(EvaluationRequest request) {
        Evaluation evaluation = new Evaluation();
        mapToEntity(request, evaluation);
        return toResponse(evaluationRepository.save(evaluation));
    }

    @Override
    public EvaluationResponse update(Long id, EvaluationRequest request) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation introuvable : " + id));
        mapToEntity(request, evaluation);
        return toResponse(evaluationRepository.save(evaluation));
    }

    @Override
    public void delete(Long id) {
        if (!evaluationRepository.existsById(id))
            throw new ResourceNotFoundException("Evaluation introuvable : " + id);
        evaluationRepository.deleteById(id);
    }

    @Override
    public List<EvaluationResponse> findByStage(Long stageId) {
        return evaluationRepository.findByStageId(stageId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void mapToEntity(EvaluationRequest request, Evaluation evaluation) {
        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        evaluation.setStage(stage);

        Encadrant encadrant = encadrantRepository.findById(request.getEncadrantId())
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        evaluation.setEncadrant(encadrant);

        if (request.getNote() != null)
            evaluation.setNote(java.math.BigDecimal.valueOf(request.getNote()));
        evaluation.setCommentaire(request.getCommentaire());
        evaluation.setTypeEval(request.getTypeEvaluation());
        if (request.getDateEval() != null)
            evaluation.setDateEval(request.getDateEval());
    }

    private EvaluationResponse toResponse(Evaluation e) {
        EvaluationResponse response = new EvaluationResponse();
        response.setId(e.getId());
        response.setNote(e.getNote() != null ? e.getNote().doubleValue() : null);
        response.setCommentaire(e.getCommentaire());
        response.setTypeEvaluation(e.getTypeEval());
        response.setDateEval(e.getDateEval());
        response.setCreatedAt(e.getCreatedAt());
        if (e.getStage() != null) {
            response.setStageId(e.getStage().getId());
            response.setStageSujet(e.getStage().getSujet());
        }
        if (e.getEncadrant() != null) {
            response.setEncadrantId(e.getEncadrant().getId());
            response.setEncadrantNom(e.getEncadrant().getNom() + " " + e.getEncadrant().getPrenom());
        }
        return response;
    }
    @Override
    public List<EvaluationResponse> getMesEvaluationsStagiaire(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Stagiaire stagiaire = stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));
        List<Stage> stages = stageRepository.findByStagiaireId(stagiaire.getId());
        return stages.stream()
                .flatMap(s -> evaluationRepository.findByStageId(s.getId()).stream())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationResponse> getMesEvaluationsEncadrant(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Encadrant encadrant = encadrantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        return evaluationRepository.findByEncadrantId(encadrant.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
}