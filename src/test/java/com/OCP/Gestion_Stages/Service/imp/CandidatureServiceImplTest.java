package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.*;
import com.OCP.Gestion_Stages.Service.EmailService;
import com.OCP.Gestion_Stages.Service.OllamaService;
import com.OCP.Gestion_Stages.Service.interfaces.ConventionServiceExtended;
import com.OCP.Gestion_Stages.domain.dto.candidature.CandidatureResponse;
import com.OCP.Gestion_Stages.domain.dto.candidature.TraiterCandidatureRequest;
import com.OCP.Gestion_Stages.domain.enums.UserRole;
import com.OCP.Gestion_Stages.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceImplTest {

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private UserRepository userRepository;
    @Mock private StagiaireRepository stagiaireRepository;
    @Mock private StageRepository stageRepository;
    @Mock private EncadrantRepository encadrantRepository;
    @Mock private DepartementRepository departementRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AnnonceStageRepository annonceRepository;
    @Mock private OllamaService ollamaService;
    @Mock private ApplicationContext applicationContext;
    @Mock private DocumentCandidatureRepository documentCandidatureRepository;
    @Mock private DocumentStagiaireRepository documentStagiaireRepository;
    @Mock private ConventionServiceExtended conventionServiceExtended;
    @Mock private EtablissementRepository etablissementRepository;
    @Mock private OnboardingChecklistRepository onboardingChecklistRepository;

    @InjectMocks
    private CandidatureServiceImpl candidatureService;

    private Candidature candidatureEnAttente;
    private TraiterCandidatureRequest acceptRequest;

    @BeforeEach
    void setUp() {
        candidatureEnAttente = new Candidature();
        candidatureEnAttente.setId(1L);
        candidatureEnAttente.setNom("Dupont");
        candidatureEnAttente.setPrenom("Ahmed");
        candidatureEnAttente.setEmail("ahmed.dupont@test.com");
        candidatureEnAttente.setTelephone("0612345678");
        candidatureEnAttente.setFiliere("Informatique");
        candidatureEnAttente.setNiveau("Bac+5");
        candidatureEnAttente.setEtablissement("ENSIAS");
        candidatureEnAttente.setSujetSouhaite("Application Web");
        candidatureEnAttente.setStatut("EN_ATTENTE");

        acceptRequest = new TraiterCandidatureRequest();
        acceptRequest.setStatut("ACCEPTEE");
        acceptRequest.setEncadrantId(10L);
        acceptRequest.setDepartementId(20L);
        acceptRequest.setDateDebut(LocalDate.of(2026, 7, 1));
        acceptRequest.setDateFin(LocalDate.of(2026, 9, 30));
        acceptRequest.setTypeStage("PFE");
        acceptRequest.setSujet("Développement web");
    }

    // ─── Helpers ───

    private void mockCandidatureFound() {
        when(candidatureRepository.findById(1L)).thenReturn(Optional.of(candidatureEnAttente));
    }

    private void mockNoExistingAccount() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
    }

    private void mockDeptAndEncadrant() {
        Departement dept = new Departement();
        dept.setId(20L);
        dept.setNom("IT");
        when(departementRepository.findById(20L)).thenReturn(Optional.of(dept));

        Encadrant enc = new Encadrant();
        enc.setId(10L);
        when(encadrantRepository.findById(10L)).thenReturn(Optional.of(enc));
    }

    // ─── Tests : traiter() ───

    @Nested
    @DisplayName("traiter() — Acceptation")
    class TraiterAcceptation {

        @Test
        @DisplayName("Doit créer User, Stagiaire, Stage et Onboarding quand ACCEPTEE")
        void accepter_creeCompteStagiaire() throws Exception {
            mockCandidatureFound();
            mockNoExistingAccount();
            mockDeptAndEncadrant();
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);

            candidatureService.traiter(1L, acceptRequest, "admin.rh");

            // User créé avec bon role
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUsername()).isEqualTo("ahmed.dupont");
            assertThat(savedUser.getEmail()).isEqualTo("ahmed.dupont@test.com");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.STAGIAIRE);
            assertThat(savedUser.getActif()).isTrue();

            // Stagiaire créé
            ArgumentCaptor<Stagiaire> stagCaptor = ArgumentCaptor.forClass(Stagiaire.class);
            verify(stagiaireRepository).save(stagCaptor.capture());
            Stagiaire savedStagiaire = stagCaptor.getValue();
            assertThat(savedStagiaire.getNom()).isEqualTo("Dupont");
            assertThat(savedStagiaire.getPrenom()).isEqualTo("Ahmed");
            assertThat(savedStagiaire.getFiliere()).isEqualTo("Informatique");

            // Stage créé avec bonnes valeurs
            ArgumentCaptor<Stage> stageCaptor = ArgumentCaptor.forClass(Stage.class);
            verify(stageRepository).save(stageCaptor.capture());
            Stage savedStage = stageCaptor.getValue();
            assertThat(savedStage.getSujet()).isEqualTo("Développement web");
            assertThat(savedStage.getDateDebut()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(savedStage.getDateFin()).isEqualTo(LocalDate.of(2026, 9, 30));

            // Onboarding créé (10 items)
            verify(onboardingChecklistRepository, times(10)).save(any(OnboardingChecklist.class));
        }

        @Test
        @DisplayName("Doit utiliser le sujet souhaité du candidat si RH ne fournit pas de sujet")
        void accepter_sujetParDefaut() throws Exception {
            mockCandidatureFound();
            mockNoExistingAccount();
            mockDeptAndEncadrant();
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);

            acceptRequest.setSujet(null); // RH ne fournit pas de sujet

            candidatureService.traiter(1L, acceptRequest, "admin.rh");

            ArgumentCaptor<Stage> stageCaptor = ArgumentCaptor.forClass(Stage.class);
            verify(stageRepository).save(stageCaptor.capture());
            assertThat(stageCaptor.getValue().getSujet()).isEqualTo("Application Web"); // sujetSouhaite du candidat
        }

        @Test
        @DisplayName("Doit envoyer un email avec les identifiants")
        void accepter_envoieEmail() throws Exception {
            mockCandidatureFound();
            mockNoExistingAccount();
            mockDeptAndEncadrant();
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);

            candidatureService.traiter(1L, acceptRequest, "admin.rh");

            // Email credentials + email notification = 2 appels
            verify(emailService, atLeast(2)).envoyerEmail(anyString(), anyString(), anyString());
        }
    }

    // ─── Tests : Garde double-traitement ───

    @Nested
    @DisplayName("traiter() — Garde double-traitement")
    class GardeDoubleTraitement {

        @Test
        @DisplayName("Ne doit PAS re-traiter une candidature déjà ACCEPTEE")
        void doubleAcceptation_ignoree() throws Exception {
            candidatureEnAttente.setStatut("ACCEPTEE"); // déjà acceptée
            mockCandidatureFound();

            CandidatureResponse response = candidatureService.traiter(1L, acceptRequest, "admin.rh");

            // Aucun compte ne doit être créé
            verify(userRepository, never()).save(any());
            verify(stagiaireRepository, never()).save(any());
            verify(stageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ne doit PAS re-traiter une candidature déjà REFUSEE")
        void doubleRefus_ignore() throws Exception {
            candidatureEnAttente.setStatut("REFUSEE"); // déjà refusée
            mockCandidatureFound();

            TraiterCandidatureRequest refusRequest = new TraiterCandidatureRequest();
            refusRequest.setStatut("ACCEPTEE"); // tentative d'accepter après refus

            candidatureService.traiter(1L, refusRequest, "admin.rh");

            verify(userRepository, never()).save(any());
        }
    }

    // ─── Tests : Collision email ───

    @Nested
    @DisplayName("creerCompteStagiaire() — Collision email")
    class CollisionEmail {

        @Test
        @DisplayName("Ne doit PAS créer de doublon si l'email existe déjà")
        void emailDejaExistant_pasDeDoblon() throws Exception {
            mockCandidatureFound();
            when(userRepository.existsByEmail("ahmed.dupont@test.com")).thenReturn(true);
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);

            candidatureService.traiter(1L, acceptRequest, "admin.rh");

            // User NE doit PAS être créé
            verify(userRepository, never()).save(any(User.class));
            // Stagiaire NE doit PAS être créé
            verify(stagiaireRepository, never()).save(any());
        }
    }

    // ─── Tests : Username unique ───

    @Nested
    @DisplayName("genererUsernameUnique()")
    class UsernameUnique {

        @Test
        @DisplayName("Génère prenom.nom si disponible")
        void usernameSimple() {
            when(userRepository.existsByUsername("ahmed.dupont")).thenReturn(false);

            String result = candidatureService.genererUsernameUnique("Ahmed", "Dupont");

            assertThat(result).isEqualTo("ahmed.dupont");
        }

        @Test
        @DisplayName("Ajoute un suffixe numérique si collision")
        void usernameSuffixe() {
            when(userRepository.existsByUsername("ahmed.dupont")).thenReturn(true);
            when(userRepository.existsByUsername("ahmed.dupont1")).thenReturn(true);
            when(userRepository.existsByUsername("ahmed.dupont2")).thenReturn(false);

            String result = candidatureService.genererUsernameUnique("Ahmed", "Dupont");

            assertThat(result).isEqualTo("ahmed.dupont2");
        }

        @Test
        @DisplayName("Supprime les caractères spéciaux du nom")
        void usernameCaracteresSpeciaux() {
            when(userRepository.existsByUsername("fatima.elamrani")).thenReturn(false);

            String result = candidatureService.genererUsernameUnique("Fatima", "El-Amrani");

            assertThat(result).isEqualTo("fatima.elamrani");
        }

        @Test
        @DisplayName("Supprime les accents et caractères non-alpha")
        void usernameAccents() {
            when(userRepository.existsByUsername("rda.benasser")).thenReturn(false);

            String result = candidatureService.genererUsernameUnique("Réda", "Ben'asser");

            // Les accents et apostrophes sont supprimés par le regex [^a-z.]
            assertThat(result).isEqualTo("rda.benasser");
        }
    }

    // ─── Tests : Mot de passe ───

    @Nested
    @DisplayName("genererMotDePasse()")
    class MotDePasse {

        @Test
        @DisplayName("Le mot de passe commence par OCP@")
        void prefixeOCP() {
            String password = candidatureService.genererMotDePasse();

            assertThat(password).startsWith("OCP@");
        }

        @Test
        @DisplayName("Le mot de passe fait 12 caractères (OCP@ + 8)")
        void longueurMotDePasse() {
            String password = candidatureService.genererMotDePasse();

            assertThat(password).hasSize(12);
        }

        @Test
        @DisplayName("Deux mots de passe générés sont différents")
        void uniqueMotDePasse() {
            String p1 = candidatureService.genererMotDePasse();
            String p2 = candidatureService.genererMotDePasse();

            assertThat(p1).isNotEqualTo(p2);
        }
    }

    // ─── Tests : Refus ───

    @Nested
    @DisplayName("traiter() — Refus")
    class TraiterRefus {

        @Test
        @DisplayName("Le refus ne crée aucun compte")
        void refuser_pasDeCompte() throws Exception {
            mockCandidatureFound();
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);

            TraiterCandidatureRequest refusRequest = new TraiterCandidatureRequest();
            refusRequest.setStatut("REFUSEE");
            refusRequest.setCommentaireRh("Profil non adapté");

            candidatureService.traiter(1L, refusRequest, "admin.rh");

            verify(userRepository, never()).save(any());
            verify(stagiaireRepository, never()).save(any());
            verify(stageRepository, never()).save(any());
            verify(onboardingChecklistRepository, never()).save(any());
        }

        @Test
        @DisplayName("Le refus met à jour le statut et le commentaire")
        void refuser_metAJourStatut() throws Exception {
            mockCandidatureFound();
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);

            TraiterCandidatureRequest refusRequest = new TraiterCandidatureRequest();
            refusRequest.setStatut("REFUSEE");
            refusRequest.setCommentaireRh("Profil non adapté");

            candidatureService.traiter(1L, refusRequest, "admin.rh");

            assertThat(candidatureEnAttente.getStatut()).isEqualTo("REFUSEE");
            assertThat(candidatureEnAttente.getCommentaireRh()).isEqualTo("Profil non adapté");
            assertThat(candidatureEnAttente.getTraitePar()).isEqualTo("admin.rh");
            assertThat(candidatureEnAttente.getTraiteAt()).isNotNull();
        }
    }

    // ─── Tests : Auto-génération convention ───

    @Nested
    @DisplayName("Auto-génération convention")
    class AutoConvention {

        @Test
        @DisplayName("Génère la convention si les 4 docs requis sont présents")
        void conventionAutoGeneree() throws Exception {
            mockCandidatureFound();
            mockNoExistingAccount();
            mockDeptAndEncadrant();
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);
            // Tous les docs requis sont présents
            when(documentStagiaireRepository.existsByStagiaireIdAndTypeDocument(any(), anyString()))
                    .thenReturn(true);

            candidatureService.traiter(1L, acceptRequest, "admin.rh");

            verify(conventionServiceExtended).generer(any());
        }

        @Test
        @DisplayName("Ne génère PAS la convention si docs manquants")
        void conventionNonGeneree() throws Exception {
            mockCandidatureFound();
            mockNoExistingAccount();
            mockDeptAndEncadrant();
            when(candidatureRepository.save(any())).thenReturn(candidatureEnAttente);
            // Docs pas tous présents
            when(documentStagiaireRepository.existsByStagiaireIdAndTypeDocument(any(), anyString()))
                    .thenReturn(false);

            candidatureService.traiter(1L, acceptRequest, "admin.rh");

            verify(conventionServiceExtended, never()).generer(any());
        }
    }

    // ─── Tests : Candidature introuvable ───

    @Test
    @DisplayName("Lève une exception si la candidature n'existe pas")
    void traiter_candidatureIntrouvable() {
        when(candidatureRepository.findById(999L)).thenReturn(Optional.empty());

        TraiterCandidatureRequest req = new TraiterCandidatureRequest();
        req.setStatut("ACCEPTEE");

        assertThatThrownBy(() -> candidatureService.traiter(999L, req, "admin"))
                .isInstanceOf(com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException.class)
                .hasMessageContaining("Candidature introuvable");
    }
}
