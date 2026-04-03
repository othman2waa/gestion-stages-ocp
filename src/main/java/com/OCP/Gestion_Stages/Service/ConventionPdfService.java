package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ConventionPdfService {

    private static final DeviceRgb OCP_GREEN = new DeviceRgb(0, 132, 61);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb BORDER_GRAY = new DeviceRgb(226, 232, 240);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_LONG = DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.FRENCH);

    public byte[] genererPdf(ConventionResponse conv) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            doc.setMargins(50, 60, 50, 60);

            // ===== EN-TÊTE =====
            Table entete = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth();

            // Gauche: infos OCP
            Cell gauche = new Cell()
                    .add(new Paragraph("OCP | SBU Mining").setFontSize(14).setBold().setFontColor(OCP_GREEN))
                    .add(new Paragraph("Direction Industrielle Mines Gantour").setFontSize(9).setBold())
                    .add(new Paragraph("Direction Capital Humain").setFontSize(9).setBold())
                    .add(new Paragraph("Développement RH").setFontSize(9).setBold())
                    .add(new Paragraph(" ").setFontSize(5))
                    .add(new Paragraph("Tél. +212 (0) 6 62 07 74 39").setFontSize(8).setFontColor(ColorConstants.DARK_GRAY))
                    .add(new Paragraph("Fax : +212 (0) 5 24 64 60 86").setFontSize(8).setFontColor(ColorConstants.DARK_GRAY))
                    .setBorder(Border.NO_BORDER);

            // Droite: date et lieu
            String dateEmission = conv.getDateEmission() != null
                    ? "Benguerir, le " + conv.getDateEmission().format(DATE_LONG)
                    : "Benguerir, le —";
            Cell droite = new Cell()
                    .add(new Paragraph(dateEmission).setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);

            entete.addCell(gauche);
            entete.addCell(droite);
            doc.add(entete);

            doc.add(new Paragraph(" ").setFontSize(6));

            // ===== NUMÉRO ET DESTINATAIRE =====
            Table refDest = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

            Cell refCell = new Cell()
                    .add(new Paragraph("MIG/H/DH - ES n° " + safe(conv.getNumero())).setFontSize(10).setBold())
                    .setBorder(Border.NO_BORDER);

            // Destinataire (stagiaire)
            String[] nomParts = safe(conv.getStagiaireNom()).split(" ");
            String nomFormate = conv.getStagiaireNom() != null
                    ? "Monsieur/Madame " + conv.getStagiaireNom().toUpperCase()
                    : "Monsieur/Madame —";
            Cell destCell = new Cell()
                    .add(new Paragraph(nomFormate).setFontSize(10).setBold())
                    .add(new Paragraph("S/C de : " + safe(conv.getStagiaireEtablissement())).setFontSize(9))
                    .add(new Paragraph("").setFontSize(4))
                    .add(new Paragraph("- MARRAKECH -").setFontSize(9).setBold())
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.LEFT);

            refDest.addCell(refCell);
            refDest.addCell(destCell);
            doc.add(refDest);

            doc.add(new Paragraph(" ").setFontSize(10));

            // ===== CORPS DE LA LETTRE =====
            String typeStageTexte = "PFE".equals(conv.getTypeStage())
                    ? "Projet de fin d'études"
                    : "PFA".equals(conv.getTypeStage())
                    ? "Projet de fin d'année"
                    : safe(conv.getTypeStage());

            doc.add(new Paragraph("Monsieur/Madame,").setFontSize(10).setMarginBottom(8));

            doc.add(new Paragraph(
                    "\t\tSuite à votre demande, nous avons l'honneur de vous faire part de notre accord pour " +
                            "l'organisation d'un " + typeStageTexte + " au sein du Groupe OCP.")
                    .setFontSize(10).setMarginBottom(12).setFirstLineIndent(20));

            // ===== TABLEAU INFORMATIONS =====
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{38, 62})).useAllAvailableWidth();
            infoTable.setMarginBottom(12);

            // Année et spécialité
            String niveauSpecialite = safe(conv.getStagiaireNiveau()) + " - " + safe(conv.getStagiaireFiliere());
            ajouterLigneInfo(infoTable, "Année d'étude et spécialité :", niveauSpecialite);

            // Période de stage
            String periode = "—";
            if (conv.getStageDebut() != null && conv.getStageFin() != null) {
                periode = "Du " + conv.getStageDebut().format(DATE_FMT) +
                        " au " + conv.getStageFin().format(DATE_FMT);
            }
            ajouterLigneInfo(infoTable, "Période de stage :", periode);

            // Direction d'accueil
            ajouterLigneInfo(infoTable, "Direction d'accueil :", safe(conv.getDepartementNom()));

            // Entité d'accueil
            ajouterLigneInfo(infoTable, "Entité d'accueil :", safe(conv.getStageSujet()));

            // Parrain de stage
            ajouterLigneInfo(infoTable, "Parrain de stage :", safe(conv.getEncadrantNom()));

            doc.add(infoTable);

            // ===== CONDITIONS GÉNÉRALES =====
            doc.add(new Paragraph("Conditions générales\t:").setFontSize(10).setBold().setMarginBottom(4));

            doc.add(new Paragraph("\u2022  Hébergement et restauration : à la charge des stagiaires")
                    .setFontSize(9).setMarginLeft(20).setMarginBottom(2));

            doc.add(new Paragraph("\u2022  Assurance : Les stagiaires doivent être assurés par leurs soins ou leur " +
                    "école contre les risques encourus durant leur séjour au sein du Groupe OCP " +
                    "(accident de travail, de trajet, maladie,...)")
                    .setFontSize(9).setMarginLeft(20).setMarginBottom(12));

            doc.add(new Paragraph(
                    "Veuillez agréer Monsieur/Madame, l'expression de nos sentiments distingués.")
                    .setFontSize(10).setMarginBottom(8));

            doc.add(new Paragraph(
                    "NB : Le stage ne peut en aucun cas être prolongé au delà de la durée contractée")
                    .setFontSize(9).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(20));

            // ===== SIGNATURE =====
            Table sigTable = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();

            Cell sigGauche = new Cell()
                    .add(new Paragraph("P. Le Président Directeur Général & p.o.,").setFontSize(9).setBold())
                    .add(new Paragraph("P. Le Responsable Développement RH,").setFontSize(9).setBold())
                    .add(new Paragraph("\n\n\n").setFontSize(8))
                    .add(new Paragraph("_______________________________").setFontSize(9))
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);

            sigTable.addCell(new Cell().setBorder(Border.NO_BORDER));
            sigTable.addCell(sigGauche);
            doc.add(sigTable);

            // ===== PIED DE PAGE =====
            doc.add(new Paragraph(" ").setFontSize(20));
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine())
                    .setMarginBottom(6));

            doc.add(new Paragraph(
                    "OCP S.A\n" +
                            "Société anonyme au capital de 8.287.500.000 DH - Registre de Commerce : 40327 " +
                            "Identification Fiscale : 02220794 - Patente n°36000670\n" +
                            "2-4, rue Al Abtal, Hay Erraha, 20 200 Casablanca, Maroc - Téléphone/Standard : " +
                            "+212 (0) 5 22 23 20 25 / +212 5 22 92 30 00 / +212 (0) 5 22 92 40 00\n" +
                            "www.ocpgroup.ma")
                    .setFontSize(7)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération PDF convention: {}", e.getMessage());
            throw new RuntimeException("Erreur génération PDF: " + e.getMessage());
        }
    }

    private void ajouterLigneInfo(Table table, String label, String valeur) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(10).setBold())
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(4).setPaddingBottom(4));
        table.addCell(new Cell()
                .add(new Paragraph(valeur).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(4).setPaddingBottom(4));
    }

    private String safe(String val) {
        return val != null && !val.isEmpty() ? val : "—";
    }
}