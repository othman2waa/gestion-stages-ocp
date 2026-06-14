package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.StageHistoriqueRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.interfaces.WorkflowService;
import com.OCP.Gestion_Stages.domain.dto.StageHistoriqueResponse;
import com.OCP.Gestion_Stages.domain.dto.WorkflowRequest;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.domain.model.StageHistorique;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {

    private final StageRepository stageRepository;
    private final StageHistoriqueRepository historiqueRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void transitionner(Long stageId, WorkflowRequest request) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found"));

        StageStatus ancienStatut = stage.getStatut();
        StageStatus nouveauStatut = request.getNouveauStatut();

        if (!ancienStatut.canTransitionTo(nouveauStatut)) {
            throw new IllegalStateException(
                    "Transition impossible : " + ancienStatut + " → " + nouveauStatut
            );
        }

        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        StageHistorique historique = StageHistorique.builder()
                .stage(stage)
                .statutPrecedent(ancienStatut)
                .statutNouveau(nouveauStatut)
                .commentaire(request.getCommentaire())
                .modifiePar(currentUser)
                .build();

        stage.setStatut(nouveauStatut);
        stageRepository.save(stage);
        historiqueRepository.save(historique);

        // Envoi email au stagiaire
        try {
            if (stage.getStagiaire() != null && stage.getStagiaire().getEmail() != null) {
                String nomComplet = stage.getStagiaire().getPrenom() + " " + stage.getStagiaire().getNom();
                String dateFin = stage.getDateFin() != null ? stage.getDateFin().toString() : "N/A";
                emailService.envoyerChangementStatut(
                        stage.getStagiaire().getEmail(),
                        nomComplet,
                        stage.getSujet(),
                        ancienStatut.name(),
                        nouveauStatut.name(),
                        request.getCommentaire()
                );
            }
        } catch (Exception e) {
            log.warn("Email non envoyé pour stage {}: {}", stageId, e.getMessage());
        }
    }

    @Override
    public List<StageHistoriqueResponse> getHistorique(Long stageId) {
        return historiqueRepository
                .findByStageIdOrderByDateModificationDesc(stageId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getTransitionsPossibles(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found"));

        return Arrays.stream(StageStatus.values())
                .filter(s -> stage.getStatut().canTransitionTo(s))
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private StageHistoriqueResponse toResponse(StageHistorique h) {
        StageHistoriqueResponse r = new StageHistoriqueResponse();
        r.setId(h.getId());
        r.setStatutPrecedent(h.getStatutPrecedent());
        r.setStatutNouveau(h.getStatutNouveau());
        r.setCommentaire(h.getCommentaire());
        r.setModifiePar(h.getModifiePar());
        r.setDateModification(h.getDateModification());
        return r;
    }
}