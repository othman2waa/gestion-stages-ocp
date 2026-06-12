package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.interfaces.FicheAppreciationService;
import com.OCP.Gestion_Stages.domain.dto.fiche.*;
import com.OCP.Gestion_Stages.domain.model.*;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import com.OCP.Gestion_Stages.exeptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FicheAppreciationServiceImpl implements FicheAppreciationService {

    private final FicheAppreciationStageRepository ficheStageRepo;
    private final FicheAppreciationStagiaireRepository ficheStagiaireRepo;
    private final StageRepository stageRepository;
    private final EncadrantRepository encadrantRepository;
    private final UserRepository userRepository;
    private final com.OCP.Gestion_Stages.Service.interfaces.NotificationService notificationService;

    // ── Fiche Stage ──

    @Override
    public FicheStageResponse createFicheStage(FicheStageRequest request, String username) {
        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        if (ficheStageRepo.existsByStageId(request.getStageId())) {
            throw new IllegalStateException("Une fiche d'appréciation de stage existe déjà pour ce stage");
        }
        Encadrant encadrant = resolveEncadrant(username);
        assertEncadrantDuStage(stage, encadrant);

        FicheAppreciationStage fiche = new FicheAppreciationStage();
        fiche.setStage(stage);
        fiche.setEncadrant(encadrant);
        mapStageFields(request, fiche);
        FicheStageResponse resp = toStageResponse(ficheStageRepo.save(fiche));
        notifierFicheRemplie(stage, encadrant, "d'appréciation du stage");
        return resp;
    }

    @Override
    public FicheStageResponse updateFicheStage(Long id, FicheStageRequest request) {
        FicheAppreciationStage fiche = ficheStageRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche introuvable"));
        mapStageFields(request, fiche);
        return toStageResponse(ficheStageRepo.save(fiche));
    }

    @Override
    public FicheStageResponse getFicheStageByStageId(Long stageId) {
        return toStageResponse(ficheStageRepo.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche introuvable pour ce stage")));
    }

    @Override
    public List<FicheStageResponse> getAllFichesStage() {
        return ficheStageRepo.findAll().stream().map(this::toStageResponse).collect(Collectors.toList());
    }

    @Override
    public List<FicheStageResponse> getMesFichesStage(String username) {
        Encadrant encadrant = resolveEncadrant(username);
        return ficheStageRepo.findByEncadrantId(encadrant.getId())
                .stream().map(this::toStageResponse).collect(Collectors.toList());
    }

    // ── Fiche Stagiaire ──

    @Override
    public FicheStagiaireResponse createFicheStagiaire(FicheStagiaireRequest request, String username) {
        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable"));
        if (ficheStagiaireRepo.existsByStageId(request.getStageId())) {
            throw new IllegalStateException("Une fiche d'appréciation stagiaire existe déjà pour ce stage");
        }
        Encadrant encadrant = resolveEncadrant(username);
        assertEncadrantDuStage(stage, encadrant);

        FicheAppreciationStagiaire fiche = new FicheAppreciationStagiaire();
        fiche.setStage(stage);
        fiche.setEncadrant(encadrant);
        mapStagiaireFields(request, fiche);
        FicheStagiaireResponse resp = toStagiaireResponse(ficheStagiaireRepo.save(fiche));
        notifierFicheRemplie(stage, encadrant, "d'appréciation du stagiaire");
        return resp;
    }

    /** Notifie le stagiaire qu'une fiche d'appréciation a été remplie par l'encadrant. */
    private void notifierFicheRemplie(Stage stage, Encadrant encadrant, String typeFiche) {
        try {
            if (stage.getStagiaire() != null && stage.getStagiaire().getUser() != null) {
                String encNom = encadrant != null
                        ? encadrant.getPrenom() + " " + encadrant.getNom() : "Votre encadrant";
                notificationService.notifierFicheRemplie(
                        stage.getStagiaire().getUser().getId(), typeFiche, encNom);
            }
        } catch (Exception ignored) { /* notification best-effort */ }
    }

    @Override
    public FicheStagiaireResponse updateFicheStagiaire(Long id, FicheStagiaireRequest request) {
        FicheAppreciationStagiaire fiche = ficheStagiaireRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche introuvable"));
        mapStagiaireFields(request, fiche);
        return toStagiaireResponse(ficheStagiaireRepo.save(fiche));
    }

    @Override
    public FicheStagiaireResponse getFicheStagiaireByStageId(Long stageId) {
        return toStagiaireResponse(ficheStagiaireRepo.findByStageId(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche introuvable pour ce stage")));
    }

    @Override
    public List<FicheStagiaireResponse> getAllFichesStagiaire() {
        return ficheStagiaireRepo.findAll().stream().map(this::toStagiaireResponse).collect(Collectors.toList());
    }

    @Override
    public List<FicheStagiaireResponse> getMesFichesStagiaire(String username) {
        Encadrant encadrant = resolveEncadrant(username);
        return ficheStagiaireRepo.findByEncadrantId(encadrant.getId())
                .stream().map(this::toStagiaireResponse).collect(Collectors.toList());
    }

    // ── Helpers ──

    /** Sécurité métier : l'encadrant ne peut renseigner une fiche que pour un stage qui lui est affecté. */
    private void assertEncadrantDuStage(Stage stage, Encadrant encadrant) {
        if (stage.getEncadrant() != null && !stage.getEncadrant().getId().equals(encadrant.getId()))
            throw new UnauthorizedException("Vous ne pouvez renseigner une fiche que pour vos propres stagiaires.");
    }

    private Encadrant resolveEncadrant(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return encadrantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
    }

    private void mapStageFields(FicheStageRequest r, FicheAppreciationStage f) {
        f.setNoteGlobale(r.getNoteGlobale());
        f.setQualiteTravail(r.getQualiteTravail());
        f.setRespectDelais(r.getRespectDelais());
        f.setInitiative(r.getInitiative());
        f.setQualiteRapport(r.getQualiteRapport());
        f.setCompetencesTechniques(r.getCompetencesTechniques());
        f.setRecommandation(r.getRecommandation());
        f.setCommentaires(r.getCommentaires());
    }

    private void mapStagiaireFields(FicheStagiaireRequest r, FicheAppreciationStagiaire f) {
        f.setAssiduite(r.getAssiduite());
        f.setPonctualite(r.getPonctualite());
        f.setComportementProfessionnel(r.getComportementProfessionnel());
        f.setEspritEquipe(r.getEspritEquipe());
        f.setCommunication(r.getCommunication());
        f.setAdaptation(r.getAdaptation());
        f.setRecommandationEmbauche(r.getRecommandationEmbauche());
        f.setCommentaires(r.getCommentaires());
    }

    private FicheStageResponse toStageResponse(FicheAppreciationStage f) {
        Stage stage = f.getStage();
        Encadrant enc = f.getEncadrant();
        String stagiaireNom = stage.getStagiaire() != null
                ? stage.getStagiaire().getPrenom() + " " + stage.getStagiaire().getNom()
                : "—";
        double moyenne = (f.getNoteGlobale() + f.getQualiteTravail() + f.getRespectDelais()
                + f.getInitiative() + f.getQualiteRapport() + f.getCompetencesTechniques()) / 6.0;

        return FicheStageResponse.builder()
                .id(f.getId())
                .stageId(stage.getId())
                .stageSujet(stage.getSujet())
                .stagiaireNom(stagiaireNom)
                .encadrantId(enc.getId())
                .encadrantNom(enc.getPrenom() + " " + enc.getNom())
                .noteGlobale(f.getNoteGlobale())
                .qualiteTravail(f.getQualiteTravail())
                .respectDelais(f.getRespectDelais())
                .initiative(f.getInitiative())
                .qualiteRapport(f.getQualiteRapport())
                .competencesTechniques(f.getCompetencesTechniques())
                .recommandation(f.getRecommandation())
                .commentaires(f.getCommentaires())
                .moyenneGenerale(Math.round(moyenne * 10.0) / 10.0)
                .createdAt(f.getCreatedAt())
                .build();
    }

    private FicheStagiaireResponse toStagiaireResponse(FicheAppreciationStagiaire f) {
        Stage stage = f.getStage();
        Encadrant enc = f.getEncadrant();
        String stagiaireNom = stage.getStagiaire() != null
                ? stage.getStagiaire().getPrenom() + " " + stage.getStagiaire().getNom()
                : "—";
        double moyenne = (f.getAssiduite() + f.getPonctualite() + f.getComportementProfessionnel()
                + f.getEspritEquipe() + f.getCommunication() + f.getAdaptation()) / 6.0;

        return FicheStagiaireResponse.builder()
                .id(f.getId())
                .stageId(stage.getId())
                .stageSujet(stage.getSujet())
                .stagiaireNom(stagiaireNom)
                .encadrantId(enc.getId())
                .encadrantNom(enc.getPrenom() + " " + enc.getNom())
                .assiduite(f.getAssiduite())
                .ponctualite(f.getPonctualite())
                .comportementProfessionnel(f.getComportementProfessionnel())
                .espritEquipe(f.getEspritEquipe())
                .communication(f.getCommunication())
                .adaptation(f.getAdaptation())
                .recommandationEmbauche(f.getRecommandationEmbauche())
                .commentaires(f.getCommentaires())
                .moyenneGenerale(Math.round(moyenne * 10.0) / 10.0)
                .createdAt(f.getCreatedAt())
                .build();
    }
}
