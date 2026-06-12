package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.Repository.AttestationStageRepository;
import com.OCP.Gestion_Stages.domain.model.AttestationStage;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.OCP.Gestion_Stages.domain.model.Stagiaire;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Génère la liste des stagiaires éligibles à la rémunération OCP par tranche.
 *
 * <p>OCP rémunère uniquement les stages PFE de niveau Bac+5. Le service RH établit
 * la liste deux fois par an, en début de juillet et de septembre, puis la transmet
 * au département concerné pour le versement. Chaque tranche couvre une fenêtre fixe :</p>
 * <ul>
 *   <li><b>Juillet N</b> : stages terminés du 1ᵉʳ sept. N-1 au 30 juin N</li>
 *   <li><b>Septembre N</b> : stages terminés du 1ᵉʳ juil. N au 31 août N</li>
 * </ul>
 * <p>Les deux fenêtres se succèdent sans trou ni chevauchement : comme la tranche
 * Juillet N débute le 1ᵉʳ septembre N-1, tout stage terminé <b>après le 31 août</b>
 * (donc hors de la tranche Septembre de l'année) bascule automatiquement dans la
 * tranche <b>Juillet de l'année suivante</b>. Toute date de fin appartient ainsi à
 * exactement une tranche, et aucun stagiaire n'est perdu.</p>
 * <p>Critères : stage PFE, niveau Bac+5, attestation de stage validée (APPROUVEE),
 * date de fin dans la fenêtre de la tranche. La durée du stage est calculée en mois.</p>
 */
@Service
@RequiredArgsConstructor
public class RemunerationService {

    private final AttestationStageRepository attestationRepository;

    /** Niveaux considérés Bac+5 (configurable). */
    @Value("${app.remuneration.niveaux:Bac+5,Master,Ingénieur}")
    private String niveauxConfig;

    @Transactional(readOnly = true)
    public Map<String, Object> genererListe(Integer annee, String trancheParam, Integer moisAdHoc) {
        LocalDate debut, fin;
        String tranche, libelle;

        if (moisAdHoc != null && moisAdHoc > 0) {
            // Mode ad-hoc : fenêtre glissante de N derniers mois
            fin = LocalDate.now();
            debut = fin.minusMonths(moisAdHoc);
            tranche = "PERSONNALISEE";
            libelle = moisAdHoc + " derniers mois";
        } else {
            String t = (trancheParam != null && !trancheParam.isBlank())
                    ? trancheParam.trim().toUpperCase() : trancheParDefaut();
            int year = (annee != null && annee > 0) ? annee : anneeParDefaut(t);
            if ("SEPTEMBRE".equals(t)) {
                tranche = "SEPTEMBRE";
                debut = LocalDate.of(year, 7, 1);
                fin = LocalDate.of(year, 8, 31);
                libelle = "Tranche Septembre " + year + " — stages terminés juillet à août " + year;
            } else {
                tranche = "JUILLET";
                debut = LocalDate.of(year - 1, 9, 1);
                fin = LocalDate.of(year, 6, 30);
                libelle = "Tranche Juillet " + year + " — stages terminés sept. " + (year - 1) + " à juin " + year;
            }
        }

        Set<String> niveaux = Arrays.stream(niveauxConfig.split(","))
                .map(s -> s.trim().toLowerCase()).collect(Collectors.toSet());

        List<Map<String, Object>> lignes = collecterEligibles(debut, fin, niveaux);

        // Tri par département puis nom (facilite la ventilation par département)
        lignes.sort(Comparator
                .comparing((Map<String, Object> m) -> String.valueOf(m.get("departement")))
                .thenComparing(m -> String.valueOf(m.get("stagiaireNom"))));

        // Regroupement par département concerné (chaque département reçoit sa liste)
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> l : lignes) {
            String dep = l.get("departement") != null ? l.get("departement").toString() : "(Non affecté)";
            grouped.computeIfAbsent(dep, k -> new ArrayList<>()).add(l);
        }
        List<Map<String, Object>> parDepartement = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : grouped.entrySet()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("departement", e.getKey());
            g.put("total", e.getValue().size());
            g.put("stagiaires", e.getValue());
            parDepartement.add(g);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("tranche", tranche);
        res.put("libelle", libelle);
        res.put("debut", debut.toString());
        res.put("fin", fin.toString());
        res.put("total", lignes.size());
        res.put("stagiaires", lignes);
        res.put("parDepartement", parDepartement);
        return res;
    }

    private List<Map<String, Object>> collecterEligibles(LocalDate debut, LocalDate fin, Set<String> niveaux) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (AttestationStage att : attestationRepository.findByStatutOrderByDateDemandeDesc("APPROUVEE")) {
            Stage stage = att.getStage();
            if (stage == null) continue;
            // 1) Stage PFE
            if (stage.getTypeStage() == null || !"PFE".equalsIgnoreCase(stage.getTypeStage().name())) continue;
            // 2) Niveau Bac+5
            Stagiaire st = stage.getStagiaire();
            if (st == null || st.getNiveau() == null
                    || !niveaux.contains(st.getNiveau().trim().toLowerCase())) continue;
            // 3) Terminé dans la fenêtre de la tranche
            LocalDate dateFin = stage.getDateFin();
            if (dateFin == null || dateFin.isBefore(debut) || dateFin.isAfter(fin)) continue;

            // Durée du stage en mois
            double dureeMois = 0;
            long jours = 0;
            if (stage.getDateDebut() != null) {
                jours = ChronoUnit.DAYS.between(stage.getDateDebut(), dateFin);
                dureeMois = Math.round((jours / 30.44) * 10.0) / 10.0;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stagiaireNom", st.getNom());
            m.put("stagiairePrenom", st.getPrenom());
            m.put("email", st.getEmail());
            m.put("cin", st.getCin());
            m.put("filiere", st.getFiliere());
            m.put("niveau", st.getNiveau());
            m.put("etablissement", st.getEtablissement() != null ? st.getEtablissement().getNom() : null);
            m.put("departement", stage.getDepartement() != null ? stage.getDepartement().getNom() : null);
            m.put("sujet", stage.getSujet());
            m.put("dateDebut", stage.getDateDebut() != null ? stage.getDateDebut().toString() : null);
            m.put("dateFin", dateFin.toString());
            m.put("dureeMois", dureeMois);
            m.put("dureeJours", jours);
            m.put("numeroAttestation", att.getNumeroAttestation());
            m.put("dateAttestation", att.getDateTraitement() != null ? att.getDateTraitement().toString() : null);
            lignes.add(m);
        }
        return lignes;
    }

    /** Tranche proposée par défaut selon le mois courant. */
    private String trancheParDefaut() {
        int m = LocalDate.now().getMonthValue();
        return (m == 8 || m == 9) ? "SEPTEMBRE" : "JUILLET";
    }

    /** Année proposée par défaut, cohérente avec la tranche par défaut. */
    private int anneeParDefaut(String tranche) {
        LocalDate now = LocalDate.now();
        int m = now.getMonthValue();
        int y = now.getYear();
        // Oct–Déc : pas de campagne avant la tranche de juillet de l'année suivante
        if ("JUILLET".equals(tranche) && m >= 10) return y + 1;
        return y;
    }
}
