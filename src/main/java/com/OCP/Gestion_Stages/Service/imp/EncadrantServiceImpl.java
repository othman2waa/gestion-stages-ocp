package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.EncadrantService;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantRequest;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantResponse;
import com.OCP.Gestion_Stages.domain.model.Departement;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.domain.model.User;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.OCP.Gestion_Stages.domain.dto.encadrant.MonProfilEncadrantResponse;
import com.OCP.Gestion_Stages.domain.model.Stage;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
@Transactional
public class EncadrantServiceImpl implements EncadrantService {

    private final EncadrantRepository encadrantRepository;
    private final DepartementRepository departementRepository;
    private final SuiviHebdomadaireRepository suiviRepository;
    private final UserRepository userRepository;
    private final StageRepository stageRepository;

    @Override
    public List<EncadrantResponse> findAll() {
        return encadrantRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EncadrantResponse findById(Long id) {
        return toResponse(encadrantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable : " + id)));
    }

    @Override
    public EncadrantResponse create(EncadrantRequest request) {
        Encadrant encadrant = new Encadrant();
        mapToEntity(request, encadrant);
        return toResponse(encadrantRepository.save(encadrant));
    }

    @Override
    public EncadrantResponse update(Long id, EncadrantRequest request) {
        Encadrant encadrant = encadrantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable : " + id));
        mapToEntity(request, encadrant);
        return toResponse(encadrantRepository.save(encadrant));
    }

    @Override
    public void delete(Long id) {
        if (!encadrantRepository.existsById(id))
            throw new ResourceNotFoundException("Encadrant introuvable : " + id);
        encadrantRepository.deleteById(id);
    }

    @Override
    public List<EncadrantResponse> findByDepartement(Long departementId) {
        return encadrantRepository.findByDepartementId(departementId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void mapToEntity(EncadrantRequest request, Encadrant encadrant) {
        encadrant.setNom(request.getNom());
        encadrant.setPrenom(request.getPrenom());
        encadrant.setEmail(request.getEmail());
        encadrant.setFonction(request.getFonction());
        if (request.getDepartementId() != null) {
            Departement departement = departementRepository.findById(request.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Departement introuvable"));
            encadrant.setDepartement(departement);
        }
    }

    private EncadrantResponse toResponse(Encadrant e) {
        EncadrantResponse response = new EncadrantResponse();
        response.setId(e.getId());
        response.setNom(e.getNom());
        response.setPrenom(e.getPrenom());
        response.setEmail(e.getEmail());
        response.setFonction(e.getFonction());
        response.setCreatedAt(e.getCreatedAt());
        if (e.getDepartement() != null) {
            response.setDepartementId(e.getDepartement().getId());
            response.setDepartementNom(e.getDepartement().getNom());
        }
        return response;
    }

    @Override
    public MonProfilEncadrantResponse getMonProfil(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Encadrant encadrant = encadrantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil encadrant introuvable"));

        List<Stage> stages = stageRepository.findByEncadrantId(encadrant.getId());

        MonProfilEncadrantResponse response = new MonProfilEncadrantResponse();
        response.setId(encadrant.getId());
        response.setNom(encadrant.getNom());
        response.setPrenom(encadrant.getPrenom());
        response.setEmail(encadrant.getEmail());
        response.setFonction(encadrant.getFonction());
        if (encadrant.getDepartement() != null)
            response.setDepartementNom(encadrant.getDepartement().getNom());
        response.setNombreStagiaires(stages.size());

        int totalSuivis = suiviRepository.findByEncadrantIdOrderByDateSuiviDesc(encadrant.getId()).size();
        response.setNombreSuivis(totalSuivis);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<MonProfilEncadrantResponse.StagiaireInfoDto> stagiairesDto = stages.stream().map(stage -> {
            MonProfilEncadrantResponse.StagiaireInfoDto dto = new MonProfilEncadrantResponse.StagiaireInfoDto();
            dto.setStageId(stage.getId());
            dto.setSujet(stage.getSujet());
            dto.setTypeStage(stage.getTypeStage() != null ? stage.getTypeStage().name() : null);
            dto.setStatut(stage.getStatut() != null ? stage.getStatut().name() : null);
            dto.setDateDebut(stage.getDateDebut() != null ? stage.getDateDebut().format(fmt) : null);
            dto.setDateFin(stage.getDateFin() != null ? stage.getDateFin().format(fmt) : null);
            dto.setNombreSuivis(suiviRepository.findByStageIdOrderBySemaineNumeroAsc(stage.getId()).size());

            if (stage.getStagiaire() != null) {
                dto.setStagiaireNom(stage.getStagiaire().getNom());
                dto.setStagiairePrenom(stage.getStagiaire().getPrenom());
                dto.setStagiaireEmail(stage.getStagiaire().getEmail());
                dto.setStagiaireFiliere(stage.getStagiaire().getFiliere());
                dto.setStagiaireNiveau(stage.getStagiaire().getNiveau());
                if (stage.getStagiaire().getEtablissement() != null)
                    dto.setStagiaireEtablissement(stage.getStagiaire().getEtablissement().getNom());
            }

            int prog = 0;
            if (stage.getStatut() != null) {
                prog = switch (stage.getStatut()) {
                    case EN_ATTENTE -> 5;
                    case DEMANDE_SOUMISE -> 15;
                    case EN_ATTENTE_VALIDATION -> 25;
                    case VALIDEE -> 35;
                    case CONVENTION_GENEREE -> 50;
                    case CONVENTION_SIGNEE -> 60;
                    case EN_COURS -> 75;
                    case EN_ATTENTE_EVALUATION -> 90;
                    case TERMINE -> 100;
                    default -> 0;
                };
            }
            dto.setProgression(prog);
            return dto;
        }).collect(Collectors.toList());

        response.setStagiaires(stagiairesDto);
        return response;
    }


}