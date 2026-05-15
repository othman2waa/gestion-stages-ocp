package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.AttestationServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.attestation.AttestationDTO;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttestationServiceImpl implements AttestationServiceExtended {

    private final AttestationStageRepository attestationRepository;
    private final StageRepository stageRepository;

    @Override
    public AttestationDTO demander(Long stageId) {
        if (attestationRepository.findByStageId(stageId).isPresent())
            throw new RuntimeException("Attestation déjà demandée");
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        AttestationStage att = new AttestationStage();
        att.setStage(stage);
        att.setStatut("EN_ATTENTE");
        return toDTO(attestationRepository.save(att));
    }

    @Override
    public AttestationDTO getMaDemande(Long stageId) {
        return attestationRepository.findByStageId(stageId)
            .map(this::toDTO).orElse(null);
    }

    @Override
    public List<AttestationDTO> getAll() {
        return attestationRepository.findAllByOrderByDateDemandeDesc()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<AttestationDTO> getEnAttente() {
        return attestationRepository.findByStatutOrderByDateDemandeDesc("EN_ATTENTE")
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AttestationDTO approuver(Long id, String username) {
        AttestationStage att = attestationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attestation introuvable"));
        att.setStatut("APPROUVEE");
        att.setDateTraitement(LocalDateTime.now());
        att.setTraitePar(username);
        att.setNumeroAttestation("ATT-" + att.getStage().getId() + "-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")));
        return toDTO(attestationRepository.save(att));
    }

    @Override
    public AttestationDTO refuser(Long id, String commentaire, String username) {
        AttestationStage att = attestationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attestation introuvable"));
        att.setStatut("REFUSEE");
        att.setDateTraitement(LocalDateTime.now());
        att.setTraitePar(username);
        att.setCommentaire(commentaire);
        return toDTO(attestationRepository.save(att));
    }

    @Override
    public AttestationDTO toDTO(AttestationStage att) {
        Stage s = att.getStage();
        return AttestationDTO.builder()
            .id(att.getId())
            .statut(att.getStatut())
            .dateDemande(att.getDateDemande())
            .dateTraitement(att.getDateTraitement())
            .traitePar(att.getTraitePar() != null ? att.getTraitePar() : "")
            .numeroAttestation(att.getNumeroAttestation() != null ? att.getNumeroAttestation() : "")
            .commentaire(att.getCommentaire() != null ? att.getCommentaire() : "")
            .stageId(s != null ? s.getId() : null)
            .stageSujet(s != null && s.getSujet() != null ? s.getSujet() : "")
            .stagiaireNom(s != null && s.getStagiaire() != null
                ? s.getStagiaire().getPrenom() + " " + s.getStagiaire().getNom() : "")
            .build();
    }
}
