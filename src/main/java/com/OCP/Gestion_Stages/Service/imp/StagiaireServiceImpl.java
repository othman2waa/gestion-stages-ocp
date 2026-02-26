package com.OCP.Gestion_Stages.Service.imp;



import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireRequest;
import com.OCP.Gestion_Stages.domain.dto.stagiaire.StagiaireResponse;
import com.OCP.Gestion_Stages.domain.model.Etablissement;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import com.OCP.Gestion_Stages.exeptions.BusinessException;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import com.OCP.Gestion_Stages.Repository.EtablissementRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Service.interfaces.StagiaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StagiaireServiceImpl implements StagiaireService {

    private final StagiaireRepository stagiaireRepository;
    private final EtablissementRepository etablissementRepository;

    @Override
    public StagiaireResponse createStagiaire(StagiaireRequest request) {
        if (stagiaireRepository.findByEmail(request.getEmail()).isPresent())
            throw new BusinessException("Email déjà utilisé : " + request.getEmail());

        Etablissement etablissement = null;
        if (request.getEtablissementId() != null)
            etablissement = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Etablissement", request.getEtablissementId()));

        Stagiaire stagiaire = Stagiaire.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .cin(request.getCin())
                .etablissement(etablissement)
                .filiere(request.getFiliere())
                .niveau(request.getNiveau())
                .build();

        return toResponse(stagiaireRepository.save(stagiaire));
    }

    @Override
    @Transactional(readOnly = true)
    public StagiaireResponse getStagiaireById(Long id) {
        return toResponse(stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public StagiaireResponse getStagiaireByEmail(String email) {
        return toResponse(stagiaireRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire introuvable : " + email)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StagiaireResponse> getAllStagiaires() {
        return stagiaireRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StagiaireResponse> searchStagiaires(String keyword) {
        return stagiaireRepository.searchByKeyword(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StagiaireResponse updateStagiaire(Long id, StagiaireRequest request) {
        Stagiaire stagiaire = stagiaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stagiaire", id));

        stagiaire.setNom(request.getNom());
        stagiaire.setPrenom(request.getPrenom());
        stagiaire.setEmail(request.getEmail());
        stagiaire.setTelephone(request.getTelephone());
        stagiaire.setCin(request.getCin());
        stagiaire.setFiliere(request.getFiliere());
        stagiaire.setNiveau(request.getNiveau());

        if (request.getEtablissementId() != null) {
            Etablissement etab = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Etablissement", request.getEtablissementId()));
            stagiaire.setEtablissement(etab);
        }

        return toResponse(stagiaireRepository.save(stagiaire));
    }

    @Override
    public void deleteStagiaire(Long id) {
        if (!stagiaireRepository.existsById(id))
            throw new ResourceNotFoundException("Stagiaire", id);
        stagiaireRepository.deleteById(id);
    }

    private StagiaireResponse toResponse(Stagiaire s) {
        return StagiaireResponse.builder()
                .id(s.getId())
                .nom(s.getNom())
                .prenom(s.getPrenom())
                .email(s.getEmail())
                .telephone(s.getTelephone())
                .cin(s.getCin())
                .etablissementNom(s.getEtablissement() != null ? s.getEtablissement().getNom() : null)
                .filiere(s.getFiliere())
                .niveau(s.getNiveau())
                .createdAt(s.getCreatedAt())
                .build();
    }
}