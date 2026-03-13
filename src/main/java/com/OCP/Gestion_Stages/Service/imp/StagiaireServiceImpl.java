package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.EtablissementRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.interfaces.StagiaireService;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireRequest;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.Etablissement;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.OCP.Gestion_Stages.Repository.ConventionRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.MonDashboardResponse;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import java.util.List;
import java.util.stream.Collectors;
import com.OCP.Gestion_Stages.domain.model.User;
import com.OCP.Gestion_Stages.domain.model.Stage;
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StagiaireServiceImpl implements StagiaireService {

    private final StagiaireRepository stagiaireRepository;
    private final EtablissementRepository etablissementRepository;
    private final EmailService emailService;
    private final StageRepository stageRepository;
    private final ConventionRepository conventionRepository;
    private final UserRepository userRepository;
    @Override
    public List<StagiaireResponse> findAll() {
        return stagiaireRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public StagiaireResponse findById(Long id) {
        return toResponse(stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable : " + id)));
    }

    @Override
    public StagiaireResponse create(StagiaireRequest request) {
        Stagiaire stagiaire = new Stagiaire();
        mapToEntity(request, stagiaire);
        StagiaireResponse response = toResponse(stagiaireRepository.save(stagiaire));

        try {
            emailService.envoyerBienvenue(
                    request.getEmail(),
                    request.getPrenom() + " " + request.getNom()
            );
        } catch (Exception e) {
            log.warn("Email de bienvenue non envoyé : {}", e.getMessage());
        }

        return response;
    }

    @Override
    public StagiaireResponse update(Long id, StagiaireRequest request) {
        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable : " + id));
        mapToEntity(request, stagiaire);
        return toResponse(stagiaireRepository.save(stagiaire));
    }

    @Override
    public void delete(Long id) {
        if (!stagiaireRepository.existsById(id))
            throw new ResourceNotFoundException("Stagiaire introuvable : " + id);
        stagiaireRepository.deleteById(id);
    }

    @Override
    public List<StagiaireResponse> search(String keyword) {
        return stagiaireRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(keyword, keyword)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void mapToEntity(StagiaireRequest request, Stagiaire stagiaire) {
        stagiaire.setNom(request.getNom());
        stagiaire.setPrenom(request.getPrenom());
        stagiaire.setEmail(request.getEmail());
        stagiaire.setTelephone(request.getTelephone());
        stagiaire.setCin(request.getCin());
        stagiaire.setFiliere(request.getFiliere());
        stagiaire.setNiveau(request.getNiveau());
        if (request.getEtablissementId() != null) {
            Etablissement etab = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Etablissement introuvable"));
            stagiaire.setEtablissement(etab);
        }
    }

    private StagiaireResponse toResponse(Stagiaire s) {
        StagiaireResponse response = new StagiaireResponse();
        response.setId(s.getId());
        response.setNom(s.getNom());
        response.setPrenom(s.getPrenom());
        response.setEmail(s.getEmail());
        response.setTelephone(s.getTelephone());
        response.setCin(s.getCin());
        response.setFiliere(s.getFiliere());
        response.setNiveau(s.getNiveau());
        response.setCreatedAt(s.getCreatedAt());
        if (s.getEtablissement() != null) {
            response.setEtablissementNom(s.getEtablissement().getNom());
            response.setEtablissementId(s.getEtablissement().getId());
        }
        return response;
    }
    @Override
    public MonDashboardResponse getMonDashboard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Stagiaire stagiaire = stagiaireRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable"));

        MonDashboardResponse dash = new MonDashboardResponse();
        dash.setStagiaireNom(stagiaire.getNom());
        dash.setStagiairePrenom(stagiaire.getPrenom());
        dash.setStagiaireEmail(stagiaire.getEmail());
        dash.setStagiaireFiliere(stagiaire.getFiliere());
        dash.setStagiaireNiveau(stagiaire.getNiveau());

        List<Stage> stages = stageRepository.findByStagiaireId(stagiaire.getId());
        if (!stages.isEmpty()) {
            Stage stage = stages.get(0);
            dash.setStageId(stage.getId());
            dash.setStageSujet(stage.getSujet());
            dash.setTypeStage(stage.getTypeStage() != null ? stage.getTypeStage().name() : null);
            dash.setStageStatut(stage.getStatut());
            dash.setDateDebut(stage.getDateDebut());
            dash.setDateFin(stage.getDateFin());
            dash.setProgression(calculerProgression(stage.getStatut()));
            if (stage.getDepartement() != null)
                dash.setDepartementNom(stage.getDepartement().getNom());
            if (stage.getEncadrant() != null) {
                dash.setEncadrantNom(stage.getEncadrant().getPrenom() + " " + stage.getEncadrant().getNom());
                dash.setEncadrantEmail(stage.getEncadrant().getEmail());
            }
            conventionRepository.findByStageId(stage.getId()).ifPresent(conv -> {
                dash.setConventionId(conv.getId());
                dash.setConventionNumero(conv.getNumero());
                dash.setConventionStatut(conv.getStatut());
                dash.setConventionDateEmission(conv.getDateEmission());
            });
        }

        return dash;
    }

    private int calculerProgression(StageStatus statut) {
        return switch (statut) {
            case EN_ATTENTE          -> 5;
            case DEMANDE_SOUMISE     -> 15;
            case EN_ATTENTE_VALIDATION -> 25;
            case VALIDEE             -> 35;
            case CONVENTION_GENEREE  -> 50;
            case CONVENTION_SIGNEE   -> 60;
            case EN_COURS            -> 75;
            case EN_ATTENTE_EVALUATION -> 90;
            case TERMINE             -> 100;
            case REJETEE, ANNULE     -> 0;
        };
    }
}