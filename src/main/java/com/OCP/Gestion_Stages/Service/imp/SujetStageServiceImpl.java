package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.SujetStageServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.sujet.SujetStageDTO;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SujetStageServiceImpl implements SujetStageServiceExtended {

    private final SujetStageRepository sujetRepository;
    private final EncadrantRepository encadrantRepository;
    private final DepartementRepository departementRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;

    @Override
    public SujetStageDTO proposer(Map<String, Object> body, String username) {
        SujetStage sujet = new SujetStage();
        sujet.setTitre((String) body.get("titre"));
        sujet.setDescription((String) body.get("description"));
        sujet.setTechnologies((String) body.get("technologies"));
        sujet.setNiveauRequis((String) body.get("niveauRequis"));
        sujet.setTypeStage((String) body.get("typeStage"));
        sujet.setStatut("PROPOSE");

        if (body.get("encadrantId") != null) {
            Long encId = Long.valueOf(body.get("encadrantId").toString());
            encadrantRepository.findById(encId).ifPresent(sujet::setEncadrant);
        } else {
            userRepository.findByUsername(username).ifPresent(user ->
                encadrantRepository.findAll().stream()
                    .filter(e -> e.getUser() != null && e.getUser().getId().equals(user.getId()))
                    .findFirst().ifPresent(sujet::setEncadrant));
        }

        if (body.get("departementId") != null) {
            Long deptId = Long.valueOf(body.get("departementId").toString());
            departementRepository.findById(deptId).ifPresent(sujet::setDepartement);
        }

        return toDTO(sujetRepository.save(sujet));
    }

    @Override
    public List<SujetStageDTO> getAll() {
        return sujetRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<SujetStageDTO> getByStatut(String statut) {
        return sujetRepository.findByStatutOrderByCreatedAtDesc(statut)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<SujetStageDTO> getMesSujets(String username) {
        var user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User introuvable"));
        var encadrant = encadrantRepository.findAll().stream()
            .filter(e -> e.getUser() != null && e.getUser().getId().equals(user.getId()))
            .findFirst().orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        return sujetRepository.findByEncadrantIdOrderByCreatedAtDesc(encadrant.getId())
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SujetStageDTO valider(Long id, String username) {
        SujetStage sujet = sujetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable"));
        sujet.setStatut("VALIDE");
        sujet.setValidatedAt(LocalDateTime.now());
        sujet.setValidatedBy(username);
        return toDTO(sujetRepository.save(sujet));
    }

    @Override
    public SujetStageDTO refuser(Long id) {
        SujetStage sujet = sujetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable"));
        sujet.setStatut("REFUSE");
        return toDTO(sujetRepository.save(sujet));
    }

    @Override
    public SujetStageDTO affecter(Long id, Map<String, Object> body, String username) {
        SujetStage sujet = sujetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable"));
        Long stageId = Long.valueOf(body.get("stageId").toString());
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));

        stage.setSujet(sujet.getTitre());
        stage.setSujetValide(true);
        stage.setSujetProposePar(sujet.getEncadrant() != null
            ? sujet.getEncadrant().getPrenom() + " " + sujet.getEncadrant().getNom()
            : username);

        if (body.get("encadrantId") != null) {
            Long encId = Long.valueOf(body.get("encadrantId").toString());
            encadrantRepository.findById(encId).ifPresent(stage::setEncadrant);
        } else if (sujet.getEncadrant() != null) {
            stage.setEncadrant(sujet.getEncadrant());
        }

        stageRepository.save(stage);
        sujet.setStatut("AFFECTE");
        sujet.setStage(stage);
        return toDTO(sujetRepository.save(sujet));
    }

    @Override
    public SujetStageDTO toDTO(SujetStage s) {
        return SujetStageDTO.builder()
            .id(s.getId())
            .titre(s.getTitre())
            .description(s.getDescription() != null ? s.getDescription() : "")
            .technologies(s.getTechnologies() != null ? s.getTechnologies() : "")
            .niveauRequis(s.getNiveauRequis() != null ? s.getNiveauRequis() : "")
            .typeStage(s.getTypeStage() != null ? s.getTypeStage() : "")
            .statut(s.getStatut())
            .encadrantId(s.getEncadrant() != null ? s.getEncadrant().getId() : null)
            .encadrantNom(s.getEncadrant() != null
                ? s.getEncadrant().getPrenom() + " " + s.getEncadrant().getNom() : "")
            .departementNom(s.getDepartement() != null ? s.getDepartement().getNom() : "")
            .stageId(s.getStage() != null ? s.getStage().getId() : null)
            .validatedBy(s.getValidatedBy() != null ? s.getValidatedBy() : "")
            .createdAt(s.getCreatedAt())
            .build();
    }
}
