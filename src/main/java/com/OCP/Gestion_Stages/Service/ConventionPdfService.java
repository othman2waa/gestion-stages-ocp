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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ConventionPdfService {

    private static final DeviceRgb OCP_BLUE = new DeviceRgb(30, 64, 175);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb BORDER_GRAY = new DeviceRgb(226, 232, 240);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] genererPdf(ConventionResponse conv) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            doc.setMargins(40, 50, 40, 50);

            // En-tête OCP
            ajouterEntete(doc, conv);

            // Titre
            doc.add(new Paragraph("CONVENTION DE STAGE")
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(OCP_BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20)
                    .setMarginBottom(5));

            doc.add(new Paragraph("N° " + safe(conv.getNumero()))
                    .setFontSize(11)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Ligne séparatrice
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine())
                    .setMarginBottom(20));

            // Section stagiaire
            ajouterSection(doc, "INFORMATIONS DU STAGIAIRE");
            Table tableStagiaire = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth();
            ajouterLigne(tableStagiaire, "Nom complet", safe(conv.getStagiaireNom()));
            ajouterLigne(tableStagiaire, "Email", safe(conv.getStagiaireEmail()));
            ajouterLigne(tableStagiaire, "CIN", safe(conv.getStagiaireCin()));
            ajouterLigne(tableStagiaire, "Filière", safe(conv.getStagiaireFiliere()));
            ajouterLigne(tableStagiaire, "Niveau", safe(conv.getStagiaireNiveau()));
            ajouterLigne(tableStagiaire, "Établissement", safe(conv.getStagiaireEtablissement()));
            doc.add(tableStagiaire);

            // Section stage
            doc.add(new Paragraph("").setMarginTop(15));
            ajouterSection(doc, "INFORMATIONS DU STAGE");
            Table tableStage = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth();
            ajouterLigne(tableStage, "Sujet", safe(conv.getStageSujet()));
            ajouterLigne(tableStage, "Type", safe(conv.getTypeStage()));
            ajouterLigne(tableStage, "Département", safe(conv.getDepartementNom()));
            ajouterLigne(tableStage, "Date de début", conv.getStageDebut() != null ? conv.getStageDebut().format(DATE_FMT) : "—");
            ajouterLigne(tableStage, "Date de fin", conv.getStageFin() != null ? conv.getStageFin().format(DATE_FMT) : "—");
            ajouterLigne(tableStage, "Encadrant", safe(conv.getEncadrantNom()));
            doc.add(tableStage);

            // Section signatures
            doc.add(new Paragraph("").setMarginTop(30));
            ajouterSection(doc, "SIGNATURES");
            Table tableSig = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

            Cell cellStagiaire = new Cell().add(new Paragraph("Le Stagiaire").setBold())
                    .add(new Paragraph("\n\n\n_______________________"))
                    .add(new Paragraph(safe(conv.getStagiaireNom())).setFontSize(9).setFontColor(ColorConstants.GRAY))
                    .setPadding(15).setBorder(new com.itextpdf.layout.borders.SolidBorder(BORDER_GRAY, 1))
;

            Cell cellEncadrant = new Cell().add(new Paragraph("L'Encadrant").setBold())
                    .add(new Paragraph("\n\n\n_______________________"))
                    .add(new Paragraph(safe(conv.getEncadrantNom())).setFontSize(9).setFontColor(ColorConstants.GRAY))
                    .setPadding(15).setBorder(new com.itextpdf.layout.borders.SolidBorder(BORDER_GRAY, 1))
;

            tableSig.addCell(cellStagiaire);
            tableSig.addCell(cellEncadrant);
            doc.add(tableSig);

            // Pied de page
            ajouterPiedDePage(doc, conv);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération PDF: {}", e.getMessage());
            throw new RuntimeException("Erreur génération PDF: " + e.getMessage());
        }
    }

    private void ajouterEntete(Document doc, ConventionResponse conv) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();

        Cell cellLogo = new Cell()
                .add(new Paragraph("OCP").setFontSize(28).setBold().setFontColor(OCP_BLUE))
                .add(new Paragraph("Office Chérifien des Phosphates").setFontSize(9).setFontColor(ColorConstants.GRAY))
                .add(new Paragraph("Direction des Ressources Humaines").setFontSize(9).setFontColor(ColorConstants.GRAY))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingBottom(10);

        Cell cellInfo = new Cell()
                .add(new Paragraph("Date d'émission : " + (conv.getDateEmission() != null ? conv.getDateEmission().format(DATE_FMT) : "—")).setFontSize(9))
                .add(new Paragraph("Statut : " + safe(conv.getStatut() != null ? conv.getStatut().name() : "")).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPaddingBottom(10);

        table.addCell(cellLogo);
        table.addCell(cellInfo);
        doc.add(table);
    }

    private void ajouterSection(Document doc, String titre) {
        doc.add(new Paragraph(titre)
                .setFontSize(11)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(OCP_BLUE)
                .setPadding(8)
                .setMarginBottom(0));
    }

    private void ajouterLigne(Table table, String label, String valeur) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold().setFontSize(10))
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(8)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(BORDER_GRAY, 1))
);
        table.addCell(new Cell()
                .add(new Paragraph(valeur).setFontSize(10))
                .setPadding(8)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(BORDER_GRAY, 1))
);
    }

    private void ajouterPiedDePage(Document doc, ConventionResponse conv) {
        doc.add(new Paragraph("").setMarginTop(30));
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine())
                .setMarginBottom(8));
        doc.add(new Paragraph("Document généré automatiquement par le système de gestion des stages OCP — " +
                java.time.LocalDate.now().format(DATE_FMT))
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private String safe(String val) {
        return val != null ? val : "—";
    }
}