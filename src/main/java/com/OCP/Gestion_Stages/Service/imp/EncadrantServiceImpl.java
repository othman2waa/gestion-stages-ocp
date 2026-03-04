package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.DepartementRepository;
import com.OCP.Gestion_Stages.Repository.EncadrantRepository;
import com.OCP.Gestion_Stages.Service.interfaces.EncadrantService;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantRequest;
import com.OCP.Gestion_Stages.domain.dto.encadrant.EncadrantResponse;
import com.OCP.Gestion_Stages.domain.model.Departement;
import com.OCP.Gestion_Stages.domain.model.Encadrant;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EncadrantServiceImpl implements EncadrantService {

    private final EncadrantRepository encadrantRepository;
    private final DepartementRepository departementRepository;

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
}