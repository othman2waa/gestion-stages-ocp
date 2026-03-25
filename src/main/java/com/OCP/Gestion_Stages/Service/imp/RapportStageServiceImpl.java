package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.RapportStageService;
import com.OCP.Gestion_Stages.domain.dto.rapport.RapportResponse;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RapportStageServiceImpl implements RapportStageService {

    private final RapportStageRepository rapportRepository;
    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;
    private final UserRepository userRepository;

    @Override
    public RapportResponse upload(Long stageId, MultipartFile file, String username) throws IOException {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable : " + stageId));

        // Remplace l'ancien rapport si existant
        rapportRepository.findByStageId(stageId).ifPresent(rapportRepository::delete);

        RapportStage rapport = new RapportStage();
        rapport.setStage(stage);
        rapport.setNomFichier(file.getOriginalFilename());
        rapport.setTypeContenu(file.getContentType());
        rapport.setTaille(file.getSize());
        rapport.setContenu(file.getBytes());

        return toResponse(rapportRepository.save(rapport));
    }

    @Override
    public RapportStage download(Long stageId) {
        return rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable pour le stage : " + stageId));
    }

    @Override
    public RapportResponse getMeta(Long stageId) {
        RapportStage rapport = rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable"));
        return toResponse(rapport);
    }

    @Override
    public void delete(Long stageId) {
        RapportStage rapport = rapportRepository.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable"));
        rapportRepository.delete(rapport);
    }

    @Override
    public List<RapportResponse> getMesRapports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Stagiaire stagiaire = stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));
        List<Stage> stages = stageRepository.findByStagiaireId(stagiaire.getId());
        return stages.stream()
                .map(s -> rapportRepository.findByStageId(s.getId()))
                .filter(java.util.Optional::isPresent)
                .map(opt -> toResponse(opt.get()))
                .collect(Collectors.toList());
    }

    private RapportResponse toResponse(RapportStage r) {
        RapportResponse res = new RapportResponse();
        res.setId(r.getId());
        res.setNomFichier(r.getNomFichier());
        res.setTypeContenu(r.getTypeContenu());
        res.setTaille(r.getTaille());
        res.setUploadedAt(r.getUploadedAt());
        if (r.getStage() != null) {
            res.setStageId(r.getStage().getId());
            res.setStageSujet(r.getStage().getSujet());
            if (r.getStage().getStagiaire() != null)
                res.setStagiaireNom(r.getStage().getStagiaire().getPrenom()
                        + " " + r.getStage().getStagiaire().getNom());
        }
        return res;
    }
}