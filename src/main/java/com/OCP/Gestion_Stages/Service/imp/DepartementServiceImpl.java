package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.DepartementRepository;
import com.OCP.Gestion_Stages.Repository.EncadrantRepository;
import com.OCP.Gestion_Stages.Repository.StageRepository;
import com.OCP.Gestion_Stages.Repository.StagiaireRepository;
import com.OCP.Gestion_Stages.Service.interfaces.DepartementService;
import com.OCP.Gestion_Stages.domain.dto.departement.DepartementRequest;
import com.OCP.Gestion_Stages.domain.dto.departement.DepartementResponse;
import com.OCP.Gestion_Stages.domain.model.Departement;
import com.OCP.Gestion_Stages.domain.enums.StageStatus;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartementServiceImpl implements DepartementService {

    private final DepartementRepository departementRepository;
    private final EncadrantRepository encadrantRepository;
    private final StageRepository stageRepository;
    private final StagiaireRepository stagiaireRepository;

    @Override
    public List<DepartementResponse> findAll() {
        return departementRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DepartementResponse> findActifs() {
        return departementRepository.findAll()
                .stream()
                .filter(d -> Boolean.TRUE.equals(d.getActif()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DepartementResponse findById(Long id) {
        return toResponse(departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département introuvable : " + id)));
    }

    @Override
    public DepartementResponse create(DepartementRequest request) {
        if (departementRepository.existsByCode(request.getCode()))
            throw new RuntimeException("Code département déjà utilisé : " + request.getCode());
        Departement d = new Departement();
        mapToEntity(request, d);
        return toResponse(departementRepository.save(d));
    }

    @Override
    public DepartementResponse update(Long id, DepartementRequest request) {
        Departement d = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département introuvable : " + id));
        mapToEntity(request, d);
        return toResponse(departementRepository.save(d));
    }

    @Override
    public void delete(Long id) {
        if (!departementRepository.existsById(id))
            throw new ResourceNotFoundException("Département introuvable : " + id);
        departementRepository.deleteById(id);
    }

    @Override
    public void toggleActif(Long id) {
        Departement d = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département introuvable : " + id));
        d.setActif(!Boolean.TRUE.equals(d.getActif()));
        departementRepository.save(d);
    }

    private void mapToEntity(DepartementRequest request, Departement d) {
        d.setCode(request.getCode());
        d.setNom(request.getNom());
        d.setResponsable(request.getResponsable());
        d.setDescription(request.getDescription());
        d.setEmail(request.getEmail());
        d.setTelephone(request.getTelephone());
        d.setLocalisation(request.getLocalisation());
        d.setActif(request.getActif() != null ? request.getActif() : true);
    }

    private DepartementResponse toResponse(Departement d) {
        DepartementResponse r = new DepartementResponse();
        r.setId(d.getId());
        r.setCode(d.getCode());
        r.setNom(d.getNom());
        r.setResponsable(d.getResponsable());
        r.setDescription(d.getDescription());
        r.setEmail(d.getEmail());
        r.setTelephone(d.getTelephone());
        r.setLocalisation(d.getLocalisation());
        r.setActif(d.getActif());
        r.setCreatedAt(d.getCreatedAt());
        r.setNombreEncadrants(
                (int) encadrantRepository.findAll().stream()
                        .filter(e -> e.getDepartement() != null && e.getDepartement().getId().equals(d.getId()))
                        .count()
        );
        r.setNombreStages(
                (int) stageRepository.findAll().stream()
                        .filter(s -> s.getDepartement() != null && s.getDepartement().getId().equals(d.getId()))
                        .count()
        );
        r.setNombreStagesEnCours(
                (int) stageRepository.findAll().stream()
                        .filter(s -> s.getDepartement() != null
                                && s.getDepartement().getId().equals(d.getId())
                                && s.getStatut() == StageStatus.EN_COURS)
                        .count()
        );

        r.setNombreStagiaires(
                (int) stagiaireRepository.findAll().stream()
                        .filter(s -> s.getDepartement() != null && s.getDepartement().getId().equals(d.getId()))
                        .count()
        );
        return r;
    }
}