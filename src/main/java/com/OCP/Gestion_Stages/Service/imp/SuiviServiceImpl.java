package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.SuiviService;
import com.OCP.Gestion_Stages.domain.dto.suivi.SuiviRequest;
import com.OCP.Gestion_Stages.domain.dto.suivi.SuiviResponse;
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
public class SuiviServiceImpl implements SuiviService {

    private final SuiviHebdomadaireRepository suiviRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;

    @Override
    public SuiviResponse create(SuiviRequest request, String username) {
        User encadrant = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));

        SuiviHebdomadaire suivi = new SuiviHebdomadaire();
        suivi.setStage(stage);
        suivi.setEncadrant(encadrant);
        mapToEntity(request, suivi);
        return toResponse(suiviRepository.save(suivi));
    }

    @Override
    public SuiviResponse update(Long id, SuiviRequest request) {
        SuiviHebdomadaire suivi = suiviRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suivi introuvable : " + id));
        mapToEntity(request, suivi);
        return toResponse(suiviRepository.save(suivi));
    }

    @Override
    public void delete(Long id) {
        if (!suiviRepository.existsById(id))
            throw new ResourceNotFoundException("Suivi introuvable : " + id);
        suiviRepository.deleteById(id);
    }

    @Override
    public List<SuiviResponse> findByStage(Long stageId) {
        return suiviRepository.findByStageIdOrderBySemaineNumeroAsc(stageId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<SuiviResponse> findByEncadrant(String username) {
        User encadrant = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return suiviRepository.findByEncadrantIdOrderByDateSuiviDesc(encadrant.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void mapToEntity(SuiviRequest request, SuiviHebdomadaire suivi) {
        suivi.setSemaineNumero(request.getSemaineNumero());
        suivi.setDateSuivi(request.getDateSuivi());
        suivi.setProgression(request.getProgression());
        suivi.setCommentaire(request.getCommentaire());
        suivi.setPointsPositifs(request.getPointsPositifs());
        suivi.setAxesAmelioration(request.getAxesAmelioration());
        suivi.setNote(request.getNote());
        if (request.getJoursPresents() != null)
            suivi.setJoursPresents(request.getJoursPresents());
        if (request.getJoursAbsences() != null)
            suivi.setJoursAbsences(request.getJoursAbsences());
        suivi.setMotifAbsence(request.getMotifAbsence());
        suivi.setTachesAssignees(request.getTachesAssignees());
        suivi.setTachesCompletees(request.getTachesCompletees());
        if (request.getTauxCompletionTaches() != null)
            suivi.setTauxCompletionTaches(request.getTauxCompletionTaches());
    }

    private SuiviResponse toResponse(SuiviHebdomadaire s) {
        SuiviResponse r = new SuiviResponse();
        r.setId(s.getId());
        r.setSemaineNumero(s.getSemaineNumero());
        r.setDateSuivi(s.getDateSuivi());
        r.setProgression(s.getProgression());
        r.setCommentaire(s.getCommentaire());
        r.setPointsPositifs(s.getPointsPositifs());
        r.setAxesAmelioration(s.getAxesAmelioration());
        r.setNote(s.getNote());
        r.setCreatedAt(s.getCreatedAt());
        if (s.getStage() != null) {
            r.setStageId(s.getStage().getId());
            r.setStageSujet(s.getStage().getSujet());
            if (s.getStage().getStagiaire() != null)
                r.setStagiaireNom(s.getStage().getStagiaire().getPrenom()
                        + " " + s.getStage().getStagiaire().getNom());
        }
        if (s.getEncadrant() != null)
            r.setEncadrantNom(s.getEncadrant().getUsername());

        r.setJoursPresents(s.getJoursPresents());
        r.setJoursAbsences(s.getJoursAbsences());
        r.setMotifAbsence(s.getMotifAbsence());
        r.setTachesAssignees(s.getTachesAssignees());
        r.setTachesCompletees(s.getTachesCompletees());
        r.setTauxCompletionTaches(s.getTauxCompletionTaches());
        return r;

    }
}