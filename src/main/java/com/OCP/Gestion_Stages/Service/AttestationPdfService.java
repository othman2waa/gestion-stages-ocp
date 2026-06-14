package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.domain.model.AttestationStage;
import com.OCP.Gestion_Stages.domain.model.Stage;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
public class AttestationPdfService {

    private static final DeviceRgb OCP_GREEN = new DeviceRgb(0, 132, 61);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_LONG = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    // ============================================================
    // FORMULAIRE DE DEMANDE (écrit par le stagiaire → donné au RH)
    // ============================================================
    public byte[] genererDemande(AttestationStage attestation) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf);
            doc.setMargins(50, 60, 50, 60);

            Stage stage = attestation.getStage();
            String stagiairNom = stage.getStagiaire() != null ? stage.getStagiaire().getNom() : "—";
            String stagiairePrenom = stage.getStagiaire() != null ? stage.getStagiaire().getPrenom() : "—";
            String stagiaireTel = stage.getStagiaire() != null ? safe(stage.getStagiaire().getTelephone()) : "—";
            String stagiaireEmail = stage.getStagiaire() != null ? safe(stage.getStagiaire().getEmail()) : "—";
            String dept = stage.getDepartement() != null ? stage.getDepartement().getNom() : "OCP Group";

            // En-tête OCP
            Table entete = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            entete.addCell(new Cell()
                    .add(new Paragraph("OCP").setFontSize(22).setBold().setFontColor(OCP_GREEN))
                    .add(new Paragraph("SBU - Mining").setFontSize(9))
                    .add(new Paragraph("Direction Industrielle Mines Gantour").setFontSize(9))
                    .add(new Paragraph("Direction Production Benguerir").setFontSize(9))
                    .setBorder(Border.NO_BORDER));
            entete.addCell(new Cell()
                    .add(new Paragraph("Benguérir, Le _______________")
                            .setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER));
            doc.add(entete);
            doc.add(new Paragraph(" ").setFontSize(8));

            // Infos stagiaire
            Table infoStagiaire = new Table(UnitValue.createPercentArray(new float[]{20, 80})).useAllAvailableWidth();
            infoStagiaire.setMarginBottom(16);
            ajouterLigneForm(infoStagiaire, "Nom", stagiairNom);
            ajouterLigneForm(infoStagiaire, "Prénom", stagiairePrenom);
            ajouterLigneForm(infoStagiaire, "Tel", stagiaireTel);
            ajouterLigneForm(infoStagiaire, "E-mail", stagiaireEmail);
            doc.add(infoStagiaire);
            doc.add(new Paragraph(" ").setFontSize(6));

            // Destinataire
            doc.add(new Paragraph("A").setFontSize(12).setBold()
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
            doc.add(new Paragraph("Le Responsable Développement RH Benguerir")
                    .setFontSize(12).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            // Objet
            doc.add(new Paragraph("Objet : Demande d'attestation de stage")
                    .setFontSize(11).setBold().setMarginBottom(16));

            // Corps
            doc.add(new Paragraph("Madame, Monsieur,").setFontSize(10).setMarginBottom(8));

            doc.add(new Paragraph(
                    "\t\tJ'ai l'honneur de vous demander de bien vouloir m'adresser une Attestation de stage " +
                            "que j'ai effectué dans l'entreprise OCP.SA, Benguerir")
                    .setFontSize(10).setFirstLineIndent(20).setMarginBottom(8));

            doc.add(new Paragraph(
                    "Du " + (stage.getDateDebut() != null ? stage.getDateDebut().format(DATE_FMT) : "....../...../.......") +
                            " au " + (stage.getDateFin() != null ? stage.getDateFin().format(DATE_FMT) : "....../...../.......") +
                            " au sein du service " + dept + " pour des raisons professionnelles.")
                    .setFontSize(10).setMarginBottom(16));

            doc.add(new Paragraph(
                    "\t\tDans l'attente d'une réponse favorable veuillez agréer, Madame, Monsieur, " +
                            "l'expression de ma haute considération.")
                    .setFontSize(10).setFirstLineIndent(20).setMarginBottom(40));

            // Signatures
            Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            sigTable.addCell(new Cell()
                    .add(new Paragraph("Visa Stagiaire").setFontSize(10).setBold())
                    .add(new Paragraph("\n\n\n_______________________").setFontSize(10))
                    .setBorder(Border.NO_BORDER));
            sigTable.addCell(new Cell()
                    .add(new Paragraph("Parrain du stage").setFontSize(10).setBold())
                    .add(new Paragraph("\n\n\n_______________________").setFontSize(10))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            doc.add(sigTable);

            // Pied de page
            doc.add(new Paragraph(" ").setFontSize(20));
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginBottom(6));
            doc.add(new Paragraph("OCP S.A - www.ocpgroup.ma")
                    .setFontSize(7).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération demande: {}", e.getMessage());
            throw new RuntimeException("Erreur: " + e.getMessage());
        }
    }

    // ============================================================
    // ATTESTATION OFFICIELLE — même format lettre que la demande
    // mais avec toutes les infos remplies automatiquement
    // ============================================================
    public byte[] genererAttestation(AttestationStage attestation) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf);
            doc.setMargins(50, 60, 50, 60);

            Stage stage = attestation.getStage();
            String nom = stage.getStagiaire() != null ? stage.getStagiaire().getNom() : "—";
            String prenom = stage.getStagiaire() != null ? stage.getStagiaire().getPrenom() : "—";
            String tel = stage.getStagiaire() != null ? safe(stage.getStagiaire().getTelephone()) : "—";
            String email = stage.getStagiaire() != null ? safe(stage.getStagiaire().getEmail()) : "—";
            String dept = stage.getDepartement() != null ? stage.getDepartement().getNom() : "OCP Group";
            String encadrant = stage.getEncadrant() != null
                    ? stage.getEncadrant().getPrenom() + " " + stage.getEncadrant().getNom() : "—";
            String typeStage = stage.getTypeStage() != null
                    ? switch (stage.getTypeStage().name()) {
                case "PFE" -> "Projet de Fin d'Études";
                case "PFA" -> "Projet de Fin d'Année";
                case "STAGE_ETE" -> "Stage d'Été";
                default -> stage.getTypeStage().name();
            } : "Stage";
            String dateDebut = stage.getDateDebut() != null ? stage.getDateDebut().format(DATE_FMT) : "—";
            String dateFin = stage.getDateFin() != null ? stage.getDateFin().format(DATE_FMT) : "—";
            String duree = "—";
            if (stage.getDateDebut() != null && stage.getDateFin() != null) {
                long jours = java.time.temporal.ChronoUnit.DAYS.between(
                        stage.getDateDebut(), stage.getDateFin());
                long mois = jours / 30;
                long joursR = jours % 30;
                duree = mois > 0
                        ? mois + " mois" + (joursR > 0 ? " et " + joursR + " jours" : "")
                        : jours + " jours";
            }
            String dateAujourdhui = java.time.LocalDate.now().format(DATE_LONG);

            // En-tête OCP — même style que la demande
            Table entete = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            entete.addCell(new Cell()
                    .add(new Paragraph("OCP").setFontSize(22).setBold().setFontColor(OCP_GREEN))
                    .add(new Paragraph("SBU - Mining").setFontSize(9))
                    .add(new Paragraph("Direction Industrielle Mines Gantour").setFontSize(9))
                    .add(new Paragraph("Direction Production Benguerir").setFontSize(9))
                    .setBorder(Border.NO_BORDER));
            entete.addCell(new Cell()
                    .add(new Paragraph("Benguérir, le " + dateAujourdhui)
                            .setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER));
            doc.add(entete);
            doc.add(new Paragraph(" ").setFontSize(8));

            // Infos stagiaire remplies automatiquement
            Table infoStagiaire = new Table(UnitValue.createPercentArray(new float[]{20, 80})).useAllAvailableWidth();
            infoStagiaire.setMarginBottom(16);
            ajouterLigneForm(infoStagiaire, "Nom", nom);
            ajouterLigneForm(infoStagiaire, "Prénom", prenom);
            ajouterLigneForm(infoStagiaire, "Tel", tel);
            ajouterLigneForm(infoStagiaire, "E-mail", email);
            doc.add(infoStagiaire);
            doc.add(new Paragraph(" ").setFontSize(6));

            // Numéro attestation
            doc.add(new Paragraph("Réf : " + safe(attestation.getNumeroAttestation()))
                    .setFontSize(9).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(8));

            // Destinataire
            doc.add(new Paragraph("A").setFontSize(12).setBold()
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
            doc.add(new Paragraph("Le Responsable Développement RH Benguerir")
                    .setFontSize(12).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            // Objet
            doc.add(new Paragraph("Objet : Attestation de stage")
                    .setFontSize(11).setBold().setMarginBottom(16));

            // Corps
            doc.add(new Paragraph("Madame, Monsieur,").setFontSize(10).setMarginBottom(8));

            doc.add(new Paragraph(
                    "\t\tNous attestons par la présente que " + prenom + " " + nom.toUpperCase() +
                            " a effectué un " + typeStage + " au sein de l'entreprise OCP.SA, Benguerir.")
                    .setFontSize(10).setFirstLineIndent(20).setMarginBottom(8));

            doc.add(new Paragraph(
                    "Du " + dateDebut + " au " + dateFin +
                            " au sein du service " + dept +
                            ", soit une durée de " + duree + ".")
                    .setFontSize(10).setMarginBottom(8));

            doc.add(new Paragraph("Sujet traité : " + safe(stage.getSujet()))
                    .setFontSize(10).setItalic().setMarginBottom(8));

            doc.add(new Paragraph("Encadrant : " + encadrant)
                    .setFontSize(10).setMarginBottom(16));

            doc.add(new Paragraph(
                    "\t\tCette attestation est délivrée à l'intéressé(e) pour servir et valoir ce que de droit.")
                    .setFontSize(10).setFirstLineIndent(20).setMarginBottom(8));

            doc.add(new Paragraph(
                    "\t\tVeuillez agréer, Madame, Monsieur, l'expression de nos sentiments distingués.")
                    .setFontSize(10).setFirstLineIndent(20).setMarginBottom(40));

            // Signatures
            Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            sigTable.addCell(new Cell()
                    .add(new Paragraph("Visa Stagiaire").setFontSize(10).setBold())
                    .add(new Paragraph("\n\n\n_______________________").setFontSize(10))
                    .setBorder(Border.NO_BORDER));
            sigTable.addCell(new Cell()
                    .add(new Paragraph("Parrain du stage").setFontSize(10).setBold())
                    .add(new Paragraph("\n\n\n_______________________").setFontSize(10))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            doc.add(sigTable);

            // Pied de page
            doc.add(new Paragraph(" ").setFontSize(20));
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginBottom(6));
            doc.add(new Paragraph(
                    "OCP S.A - Société anonyme au capital de 8.287.500.000 DH - RC : 40327\n" +
                            "2-4, rue Al Abtal, Hay Erraha, 20 200 Casablanca, Maroc - www.ocpgroup.ma")
                    .setFontSize(7).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération attestation: {}", e.getMessage());
            throw new RuntimeException("Erreur: " + e.getMessage());
        }
    }

    private void ajouterLigneForm(Table table, String label, String valeur) {
        table.addCell(new Cell()
                .add(new Paragraph(label + "  :").setFontSize(10).setBold())
                .setBorder(Border.NO_BORDER).setPaddingTop(4).setPaddingBottom(4));
        table.addCell(new Cell()
                .add(new Paragraph(valeur).setFontSize(10))
                .setBorder(Border.NO_BORDER).setPaddingTop(4).setPaddingBottom(4));
    }

    private String safe(String val) { return val != null && !val.isEmpty() ? val : "—"; }
}