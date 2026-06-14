package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.domain.model.Stage;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
public class LettreAccordPdfService {

    private static final DeviceRgb OCP_GREEN = new DeviceRgb(0, 132, 61);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_LONG = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    public byte[] genererLettreAccord(Stage stage) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf);
            doc.setMargins(40, 55, 40, 55);

            var stagiaire = stage.getStagiaire();
            String nom = stagiaire != null ? stagiaire.getNom() : "—";
            String prenom = stagiaire != null ? stagiaire.getPrenom() : "—";
            String etablissement = stagiaire != null && stagiaire.getEtablissement() != null
                    ? stagiaire.getEtablissement().getNom() : "—";
            String filiere = stagiaire != null ? safe(stagiaire.getFiliere()) : "—";
            String niveau = stagiaire != null ? safe(stagiaire.getNiveau()) : "—";
            String dept = stage.getDepartement() != null ? stage.getDepartement().getNom() : "Direction du Site Gantour";
            String encadrant = stage.getEncadrant() != null
                    ? stage.getEncadrant().getPrenom() + " " + stage.getEncadrant().getNom() : "—";
            String dateDebut = stage.getDateDebut() != null ? stage.getDateDebut().format(DATE_FMT) : "—";
            String dateFin = stage.getDateFin() != null ? stage.getDateFin().format(DATE_FMT) : "—";

            // ── En-tête OCP
            Table entete = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth();
            entete.addCell(new Cell()
                    .add(new Paragraph("OCP").setFontSize(22).setBold().setFontColor(OCP_GREEN))
                    .add(new Paragraph("Direction Industrielle Mines Gantour").setFontSize(9).setBold())
                    .add(new Paragraph("Direction Capital Humain").setFontSize(9))
                    .add(new Paragraph("Développement RH").setFontSize(9))
                    .add(new Paragraph(" ").setFontSize(4))
                    .add(new Paragraph("Tél. +212 (0) 6 62 07 74 39").setFontSize(8))
                    .add(new Paragraph("Fax : +212 (0) 5 24 64 60 86").setFontSize(8))
                    .setBorder(Border.NO_BORDER));
            entete.addCell(new Cell()
                    .add(new Paragraph("Benguerir, le " + LocalDate.now().format(DATE_LONG))
                            .setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER));
            doc.add(entete);

            doc.add(new Paragraph(" ").setFontSize(8));

            // ── Référence
            String ref = "MIG/H/DH - ES n° " + (stage.getId() != null ? stage.getId() : "___") + "/B/" + LocalDate.now().getYear();
            doc.add(new Paragraph(ref).setFontSize(10).setBold());

            doc.add(new Paragraph(" ").setFontSize(6));

            // ── Destinataire
            doc.add(new Paragraph("Monsieur/Madame " + prenom + " " + nom)
                    .setFontSize(11).setBold().setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("S/C de : " + etablissement)
                    .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("- MARRAKECH -")
                    .setFontSize(10).setBold().setTextAlignment(TextAlignment.RIGHT));

            doc.add(new Paragraph(" ").setFontSize(10));

            // ── Corps
            doc.add(new Paragraph("Monsieur/Madame,").setFontSize(11));
            doc.add(new Paragraph(
                    "        Suite à votre demande, nous avons l'honneur de vous faire part de notre accord pour " +
                    "l'organisation d'un Projet de fin d'études au sein du Groupe OCP.")
                    .setFontSize(11).setMarginTop(8));

            doc.add(new Paragraph(" ").setFontSize(6));

            // ── Tableau infos
            Table infos = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth();
            infos.setMarginLeft(20);
            addInfoRow(infos, "Année d'étude et spécialité", niveau + " - " + filiere);
            addInfoRow(infos, "Période de stage", "Du " + dateDebut + " au " + dateFin);
            addInfoRow(infos, "Direction d'accueil", "Direction du Site Gantour");
            addInfoRow(infos, "Entité d'accueil", dept);
            addInfoRow(infos, "Parrain de stage", encadrant);
            doc.add(infos);

            doc.add(new Paragraph(" ").setFontSize(8));

            // ── Conditions générales
            doc.add(new Paragraph("Conditions générales :").setFontSize(11).setBold());
            com.itextpdf.layout.element.List conditions = new com.itextpdf.layout.element.List()
                    .setSymbolIndent(12).setListSymbol("• ").setFontSize(10).setMarginLeft(20);
            conditions.add(new ListItem("Hébergement et restauration : à la charge des stagiaires"));
            conditions.add(new ListItem("Assurance : Les stagiaires doivent être assurés par leurs soins ou leur " +
                    "école contre les risques encourus durant leur séjour au sein du Groupe OCP " +
                    "(accident de travail, de trajet, maladie,...)"));
            doc.add(conditions);

            doc.add(new Paragraph(" ").setFontSize(8));
            doc.add(new Paragraph("        Veuillez agréer Monsieur/Madame, l'expression de nos sentiments distingués.")
                    .setFontSize(11));

            doc.add(new Paragraph(" ").setFontSize(6));
            doc.add(new Paragraph("NB : Le stage ne peut en aucun cas être prolongé au-delà de la durée contractée")
                    .setFontSize(9).setItalic());

            doc.add(new Paragraph(" ").setFontSize(14));

            // ── Signature
            doc.add(new Paragraph("P. Le Président Directeur Général & p.o.,")
                    .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("P. Le Responsable Développement RH,")
                    .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

            doc.add(new Paragraph(" ").setFontSize(30));

            // ── Pied de page
            doc.add(new Paragraph("OCP S.A").setFontSize(7).setBold());
            doc.add(new Paragraph(
                    "Société anonyme au capital de 8.287.500.000 DH - Registre de Commerce : 40327 " +
                    "Identification Fiscale : 02220794 - Patente n°36000670")
                    .setFontSize(6).setFontColor(new DeviceRgb(120, 120, 120)));
            doc.add(new Paragraph("www.ocpgroup.ma")
                    .setFontSize(6).setFontColor(OCP_GREEN));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération lettre accord", e);
            throw new RuntimeException("Erreur génération lettre d'accord PFE", e);
        }
    }

    private void addInfoRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setFontSize(10).setBold())
                .setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(": " + value).setFontSize(10))
                .setBorder(Border.NO_BORDER));
    }

    private String safe(String s) { return s == null ? "—" : s; }
}
