package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.CandidatureRepository;
import com.OCP.Gestion_Stages.Repository.DocumentCandidatureRepository;
import com.OCP.Gestion_Stages.Repository.DocumentStagiaireRepository;
import com.OCP.Gestion_Stages.domain.model.Candidature;
import com.OCP.Gestion_Stages.domain.model.DocumentCandidature;
import com.OCP.Gestion_Stages.domain.model.DocumentStagiaire;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migration d'UNE ligne bytea → disque, chacune dans sa propre transaction.
 * La transaction garde la session ouverte (lecture du byte[] même si LAZY), et
 * une exception ne rollback que cette ligne (le bytea est alors préservé).
 */
@Service
@RequiredArgsConstructor
public class StorageMigrationService {

    private final DocumentStagiaireRepository documentStagiaireRepository;
    private final DocumentCandidatureRepository documentCandidatureRepository;
    private final CandidatureRepository candidatureRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public boolean migrerDocStagiaire(Long id) {
        DocumentStagiaire d = documentStagiaireRepository.findById(id).orElse(null);
        if (d == null || d.getContenu() == null || d.getCheminFichier() != null) return false;
        d.setCheminFichier(fileStorageService.store(d.getContenu(), d.getNomFichier()));
        d.setContenu(null);
        documentStagiaireRepository.save(d);
        return true;
    }

    @Transactional
    public boolean migrerDocCandidature(Long id) {
        DocumentCandidature d = documentCandidatureRepository.findById(id).orElse(null);
        if (d == null || d.getContenu() == null || d.getCheminFichier() != null) return false;
        d.setCheminFichier(fileStorageService.store(d.getContenu(), d.getNomFichier()));
        d.setContenu(null);
        documentCandidatureRepository.save(d);
        return true;
    }

    @Transactional
    public boolean migrerCvCandidature(Long id) {
        Candidature c = candidatureRepository.findById(id).orElse(null);
        if (c == null || c.getCvContenu() == null || c.getCvChemin() != null) return false;
        String nom = c.getCvNomFichier() != null ? c.getCvNomFichier() : "cv.pdf";
        c.setCvChemin(fileStorageService.store(c.getCvContenu(), nom));
        c.setCvContenu(null);
        candidatureRepository.save(c);
        return true;
    }
}
