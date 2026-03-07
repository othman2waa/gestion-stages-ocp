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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StagiaireServiceImpl implements StagiaireService {

    private final StagiaireRepository stagiaireRepository;
    private final EtablissementRepository etablissementRepository;
    private final EmailService emailService;

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
}