package com.OCP.Gestion_Stages.Service;

import com.OCP.Gestion_Stages.domain.dto.convention.ConventionResponse;
import com.OCP.Gestion_Stages.domain.enums.ConventionStatus;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.barcodes.BarcodeQRCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ConventionPdfService {

    private static final DeviceRgb OCP_GREEN = new DeviceRgb(0, 132, 61);
    private static final DeviceRgb OCP_DARK = new DeviceRgb(0, 59, 28);
    private static final DeviceRgb OCP_ORANGE = new DeviceRgb(244, 121, 32);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb GREEN_TINT = new DeviceRgb(240, 250, 244);
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
            doc.setMargins(30, 48, 28, 48);

            // ===== BANDEAU D'EN-TÊTE OCP =====
            Table banner = new Table(UnitValue.createPercentArray(new float[]{52, 48})).useAllAvailableWidth();
            banner.setBackgroundColor(OCP_GREEN).setMarginBottom(10);

            Cell brandCell = new Cell().setBorder(Border.NO_BORDER).setPadding(9);
            Image logo = chargerLogo();
            if (logo != null) {
                logo.setHeight(28);
                Cell chip = new Cell().setBackgroundColor(WHITE).setPadding(4).setBorder(Border.NO_BORDER);
                chip.add(logo);
                Table chipWrap = new Table(1);
                chipWrap.addCell(chip);
                chipWrap.setHorizontalAlignment(HorizontalAlignment.LEFT);
                brandCell.add(chipWrap);
            } else {
                brandCell.add(new Paragraph("OCP Group").setFontColor(WHITE).setBold().setFontSize(18));
            }

            Cell titleCell = new Cell().setBorder(Border.NO_BORDER).setPadding(9)
                    .setTextAlignment(TextAlignment.RIGHT);
            titleCell.add(new Paragraph("CONVOCATION DE STAGE")
                    .setFontColor(WHITE).setBold().setFontSize(15));
            titleCell.add(new Paragraph("N° " + safe(conv.getNumero()))
                    .setFontColor(WHITE).setFontSize(9.5f));
            titleCell.add(badgeStatut(conv.getStatut()));

            banner.addCell(brandCell);
            banner.addCell(titleCell);
            doc.add(banner);

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

            doc.add(new Paragraph(" ").setFontSize(4));

            // ===== CORPS DE LA LETTRE =====
            String typeStageTexte = "PFE".equals(conv.getTypeStage())
                    ? "Projet de fin d'études"
                    : "PFA".equals(conv.getTypeStage())
                    ? "Projet de fin d'année"
                    : safe(conv.getTypeStage());

            doc.add(new Paragraph("Monsieur/Madame,").setFontSize(10).setMarginBottom(6));

            doc.add(new Paragraph(
                    "\t\tSuite à votre demande, nous avons l'honneur de vous faire part de notre accord pour " +
                            "l'organisation d'un " + typeStageTexte + " au sein du Groupe OCP.")
                    .setFontSize(10).setMarginBottom(8).setFirstLineIndent(20));

            // ===== TABLEAU INFORMATIONS =====
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{38, 62})).useAllAvailableWidth();
            infoTable.setMarginBottom(8).setBorder(new SolidBorder(BORDER_GRAY, 0.7f));
            // En-tête de section
            infoTable.addCell(new Cell(1, 2)
                    .add(new Paragraph("INFORMATIONS DU STAGE").setFontColor(WHITE).setBold().setFontSize(9.5f))
                    .setBackgroundColor(OCP_GREEN).setPadding(6).setBorder(Border.NO_BORDER));

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

            // Sujet du stage
            ajouterLigneInfo(infoTable, "Sujet du stage :", safe(conv.getStageSujet()));

            // Entité / service d'accueil
            ajouterLigneInfo(infoTable, "Entité d'accueil :", safe(conv.getEntiteAccueil()));

            // Parrain de stage
            ajouterLigneInfo(infoTable, "Parrain de stage :", safe(conv.getEncadrantNom()));

            doc.add(infoTable);

            // ===== CONDITIONS GÉNÉRALES =====
            doc.add(new Paragraph("Conditions générales\t:").setFontSize(10).setBold().setMarginBottom(3));

            doc.add(new Paragraph("\u2022  Hébergement et restauration : à la charge des stagiaires")
                    .setFontSize(9).setMarginLeft(20).setMarginBottom(2));

            doc.add(new Paragraph("\u2022  Assurance : Les stagiaires doivent être assurés par leurs soins ou leur " +
                    "école contre les risques encourus durant leur séjour au sein du Groupe OCP " +
                    "(accident de travail, de trajet, maladie,...)")
                    .setFontSize(9).setMarginLeft(20).setMarginBottom(8));

            doc.add(new Paragraph(
                    "Veuillez agréer Monsieur/Madame, l'expression de nos sentiments distingués.")
                    .setFontSize(10).setMarginBottom(6));

            doc.add(new Paragraph(
                    "NB : Le stage ne peut en aucun cas être prolongé au delà de la durée contractée")
                    .setFontSize(9).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(8));

            // ===== SIGNATURE + QR DE VÉRIFICATION (même rangée) =====
            Cell qrCell = new Cell().setBorder(Border.NO_BORDER);
            try {
                String payload = "OCP-CONV:" + safe(conv.getNumero())
                        + "|stage:" + conv.getStageId()
                        + "|" + (conv.getStatut() != null ? conv.getStatut().name() : "");
                Image qrImg = new Image(new BarcodeQRCode(payload).createFormXObject(ColorConstants.BLACK, pdf))
                        .setWidth(58).setHeight(58);
                qrCell.add(qrImg);
                qrCell.add(new Paragraph("Vérification réf. " + safe(conv.getNumero()))
                        .setFontSize(6.5f).setFontColor(ColorConstants.GRAY));
            } catch (Exception e) {
                log.warn("QR convention non généré: {}", e.getMessage());
            }

            Table sigTable = new Table(UnitValue.createPercentArray(new float[]{45, 55})).useAllAvailableWidth();
            Cell sigGauche = new Cell()
                    .add(new Paragraph("P. Le Président Directeur Général & p.o.,").setFontSize(9).setBold())
                    .add(new Paragraph("P. Le Responsable Développement RH,").setFontSize(9).setBold())
                    .add(new Paragraph("\n\n").setFontSize(8))
                    .add(new Paragraph("_______________________________").setFontSize(9))
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);

            sigTable.addCell(qrCell);
            sigTable.addCell(sigGauche);
            doc.add(sigTable);

            // ===== PIED DE PAGE =====
            doc.add(new Paragraph(" ").setFontSize(6));
            com.itextpdf.kernel.pdf.canvas.draw.SolidLine footerLine =
                    new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.7f);
            footerLine.setColor(OCP_GREEN);
            doc.add(new LineSeparator(footerLine).setMarginBottom(5));

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
        boolean pair = (table.getNumberOfRows() % 2) == 0;
        table.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(9.5f).setBold().setFontColor(OCP_DARK))
                .setBackgroundColor(GREEN_TINT)
                .setBorder(Border.NO_BORDER)
                .setPadding(4));
        table.addCell(new Cell()
                .add(new Paragraph(valeur).setFontSize(9.5f))
                .setBackgroundColor(pair ? LIGHT_GRAY : WHITE)
                .setBorder(Border.NO_BORDER)
                .setPadding(4));
    }

    /** Charge le logo OCP depuis les ressources (src/main/resources/ocp-logo.png). */
    private Image chargerLogo() {
        try (var is = getClass().getResourceAsStream("/ocp-logo.png")) {
            if (is == null) return null;
            return new Image(ImageDataFactory.create(is.readAllBytes()));
        } catch (Exception e) {
            log.warn("Logo OCP non chargé: {}", e.getMessage());
            return null;
        }
    }

    /** Badge coloré reflétant le statut de la convention. */
    private Paragraph badgeStatut(ConventionStatus statut) {
        String s = statut != null ? statut.name() : "";
        String label;
        DeviceRgb bg;
        switch (s) {
            case "SIGNEE":  label = "SIGNÉE";    bg = new DeviceRgb(22, 163, 74); break;
            case "GENEREE": label = "GÉNÉRÉE";   bg = OCP_ORANGE; break;
            case "ANNULEE": label = "ANNULÉE";   bg = new DeviceRgb(220, 38, 38); break;
            default:        label = "BROUILLON"; bg = new DeviceRgb(100, 116, 139);
        }
        return new Paragraph(label)
                .setFontColor(WHITE).setBold().setFontSize(8)
                .setBackgroundColor(bg).setPadding(3).setMarginTop(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setWidth(72).setHorizontalAlignment(HorizontalAlignment.RIGHT);
    }

    private String safe(String val) {
        return val != null && !val.isEmpty() ? val : "—";
    }
}