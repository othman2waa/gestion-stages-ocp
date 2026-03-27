package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.AnnonceStageRepository;
import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.Service.interfaces.AnnonceService;
import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceRequest;
import com.OCP.Gestion_Stages.domain.dto.annonce.AnnonceResponse;
import com.OCP.Gestion_Stages.domain.model.AnnonceStage;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnonceServiceImpl implements AnnonceService {

    private final AnnonceStageRepository annonceRepository;
    private final CandidatureRepository candidatureRepository;

    @Override
    public AnnonceResponse create(AnnonceRequest request, String username) {
        AnnonceStage annonce = new AnnonceStage();
        mapToEntity(request, annonce);
        annonce.setCreatedPar(username);
        return toResponse(annonceRepository.save(annonce));
    }

    @Override
    public AnnonceResponse update(Long id, AnnonceRequest request) {
        AnnonceStage annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce introuvable : " + id));
        mapToEntity(request, annonce);
        return toResponse(annonceRepository.save(annonce));
    }

    @Override
    public void delete(Long id) {
        if (!annonceRepository.existsById(id))
            throw new ResourceNotFoundException("Annonce introuvable : " + id);
        annonceRepository.deleteById(id);
    }

    @Override
    public List<AnnonceResponse> findAll() {
        return annonceRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<AnnonceResponse> findActives() {
        return annonceRepository.findByActifTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public AnnonceResponse findById(Long id) {
        return toResponse(annonceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce introuvable : " + id)));
    }

    @Override
    public void toggleActif(Long id) {
        AnnonceStage annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce introuvable : " + id));
        annonce.setActif(!annonce.getActif());
        annonceRepository.save(annonce);
    }

    private void mapToEntity(AnnonceRequest request, AnnonceStage annonce) {
        annonce.setTitre(request.getTitre());
        annonce.setDescription(request.getDescription());
        annonce.setCompetencesRequises(request.getCompetencesRequises());
        annonce.setDepartement(request.getDepartement());
        annonce.setTypeStage(request.getTypeStage());
        annonce.setNiveauRequis(request.getNiveauRequis());
        annonce.setFiliereRequise(request.getFiliereRequise());
        annonce.setNombrePostes(request.getNombrePostes() != null ? request.getNombrePostes() : 1);
        annonce.setDateLimite(request.getDateLimite());
        annonce.setActif(request.getActif() != null ? request.getActif() : true);
    }

    private AnnonceResponse toResponse(AnnonceStage a) {
        AnnonceResponse r = new AnnonceResponse();
        r.setId(a.getId());
        r.setTitre(a.getTitre());
        r.setDescription(a.getDescription());
        r.setCompetencesRequises(a.getCompetencesRequises());
        r.setDepartement(a.getDepartement());
        r.setTypeStage(a.getTypeStage());
        r.setNiveauRequis(a.getNiveauRequis());
        r.setFiliereRequise(a.getFiliereRequise());
        r.setNombrePostes(a.getNombrePostes());
        r.setDateLimite(a.getDateLimite());
        r.setActif(a.getActif());
        r.setCreatedAt(a.getCreatedAt());
        r.setCreatedPar(a.getCreatedPar());
        r.setNombreCandidatures(
                (int) candidatureRepository.findAllByOrderByCreatedAtDesc()
                        .stream().filter(c -> a.getId().equals(c.getAnnonceId())).count()
        );
        return r;
    }
}