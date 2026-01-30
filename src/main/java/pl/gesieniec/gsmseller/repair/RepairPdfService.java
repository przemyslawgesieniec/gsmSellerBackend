package pl.gesieniec.gsmseller.repair;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

@Service
@RequiredArgsConstructor
public class RepairPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @SneakyThrows
    public byte[] generateRepairReceiptPdf(Repair repair) {
        return generatePdf(repair, "POKWITOWANIE PRZYJĘCIA TELEFONU NA SERWIS");
    }

    @SneakyThrows
    public byte[] generateRepairHandoverPdf(Repair repair) {
        return generatePdf(repair, "POTWIERDZENIE ODBIORU TELEFONU Z SERWISU");
    }

    @SneakyThrows
    private byte[] generatePdf(Repair repair, String title) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        
        PdfFont font = PdfFontFactory.createFont("/fonts/Fira_Sans/FiraSans-Regular.ttf",
            "Identity-H", PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);

        PdfPage page = pdfDoc.addNewPage(PageSize.A4);
        Rectangle pageSize = page.getPageSize();
        float halfHeight = pageSize.getHeight() / 2;

        // Egzemplarz 1 (Góra)
        Rectangle rectUpper = new Rectangle(0, halfHeight, pageSize.getWidth(), halfHeight);
        Canvas canvasUpper = new Canvas(page, rectUpper);
        drawContent(canvasUpper, repair, title, font, rectUpper);
        canvasUpper.close();

        // Egzemplarz 2 (Dół)
        Rectangle rectLower = new Rectangle(0, 0, pageSize.getWidth(), halfHeight);
        Canvas canvasLower = new Canvas(page, rectLower);
        drawContent(canvasLower, repair, title, font, rectLower);
        canvasLower.close();

        // Linia przerywana
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        pdfCanvas.setLineDash(3, 3);
        pdfCanvas.moveTo(0, halfHeight);
        pdfCanvas.lineTo(pageSize.getWidth(), halfHeight);
        pdfCanvas.stroke();

        pdfDoc.close();
        return out.toByteArray();
    }

    @SneakyThrows
    private void drawContent(Canvas canvas, Repair repair, String title, PdfFont font, Rectangle rootRect) {
        canvas.setFont(font);

        // Logo
        drawLogo(canvas, rootRect);

        // Tytuł dokumentu
        canvas.add(new Paragraph(title)
            .setBold()
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(30));

        // Data
        canvas.add(new Paragraph("Data: " + repair.getCreateDateTime().format(DATE_FORMATTER))
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontSize(9));

        // Dane urządzenia
        canvas.add(new Paragraph("Dane urządzenia:").setBold().setUnderline().setFontSize(10).setMarginTop(5));
        
        float[] colWidths = {120f, 300f};
        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(9);

        addTableRow(table, "Urządzenie:", repair.getName());
        addTableRow(table, "IMEI:", repair.getImei());
        addTableRow(table, "Kolor:", repair.getColor());
        addTableRow(table, "PIN/Hasło:", repair.getPinPassword());
        
        canvas.add(table);

        // Status-specific content
        if (repair.getStatus() == RepairStatus.NAPRAWIONY) {
            canvas.add(new Paragraph("\nOpis naprawy:").setBold().setFontSize(10));
            canvas.add(new Paragraph(repair.getRepairOrderDescription() != null ? repair.getRepairOrderDescription() : "---")
                .setFontSize(9));
            if (repair.getRepairPrice() != null) {
                canvas.add(new Paragraph("Koszt naprawy: " + repair.getRepairPrice() + " zł")
                    .setBold().setFontSize(10));
            }
            canvas.add(new Paragraph("\nPotwierdzam odbiór sprawnego urządzenia.")
                .setFontSize(9).setItalic());
        } else if (repair.getStatus() == RepairStatus.ANULOWANY) {
            canvas.add(new Paragraph("\nStatus: ANULOWANO").setBold().setFontSize(10));
            canvas.add(new Paragraph("Naprawa została anulowana na prośbę klienta lub z przyczyn technicznych.")
                .setFontSize(9));
            canvas.add(new Paragraph("\nPotwierdzam odbiór nienaprawionego urządzenia.")
                .setFontSize(9).setItalic());
        } else if (repair.getStatus() == RepairStatus.NIE_DO_NAPRAWY) {
            canvas.add(new Paragraph("\nStatus: NIE DO NAPRAWY").setBold().setFontSize(10));
            canvas.add(new Paragraph("Urządzenie zostało sprawdzone, jednak naprawa jest niemożliwa lub nieopłacalna.")
                .setFontSize(9));
            canvas.add(new Paragraph("\nPotwierdzam odbiór nienaprawionego urządzenia.")
                .setFontSize(9).setItalic());
        } else {
            // Domyślnie (np. DO_NAPRAWY - pokwitowanie przyjęcia)
            canvas.add(new Paragraph("\nOpis uszkodzenia:").setBold().setFontSize(10));
            canvas.add(new Paragraph(repair.getDamageDescription() != null ? repair.getDamageDescription() : "---")
                .setFontSize(9));
            canvas.add(new Paragraph("Przewidywany koszt: " + (repair.getRepairPrice() != null ? repair.getRepairPrice() + " zł" : "do ustalenia"))
                .setFontSize(10));
        }

        // Podpisy
        drawSignatures(canvas);

        // Stopka
        drawFooter(canvas, rootRect);
    }

    @SneakyThrows
    private void drawLogo(Canvas canvas, Rectangle rootRect) {
        try (InputStream is = getClass().getResourceAsStream("/imgs/logo_gsm.png")) {
            if (is != null) {
                ImageData imageData = ImageDataFactory.create(is.readAllBytes());
                Image logo = new Image(imageData);
                logo.scaleToFit(100, 40);
                logo.setFixedPosition(rootRect.getLeft() + 40, rootRect.getTop() - 50);
                canvas.add(logo);
            }
        }
    }

    private void drawSignatures(Canvas canvas) {
        Table signatureTable = new Table(new float[]{1, 1});
        signatureTable.setWidth(UnitValue.createPercentValue(100));
        signatureTable.setMarginTop(30);

        signatureTable.addCell(new Cell().add(new Paragraph("........................................\nPodpis klienta")
            .setTextAlignment(TextAlignment.CENTER).setFontSize(8)).setBorder(Border.NO_BORDER));
        signatureTable.addCell(new Cell().add(new Paragraph("........................................\nPieczątka i podpis serwisu")
            .setTextAlignment(TextAlignment.CENTER).setFontSize(8)).setBorder(Border.NO_BORDER));

        canvas.add(signatureTable);
    }

    private void drawFooter(Canvas canvas, Rectangle rootRect) {
        Paragraph footerLine = new Paragraph().setBorderTop(new SolidBorder(ColorConstants.BLACK, 0.5f));
        footerLine.setFixedPosition(rootRect.getLeft() + 40, rootRect.getBottom() + 60, rootRect.getWidth() - 80);
        canvas.add(footerLine);

        Table table = new Table(new float[]{1, 1, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(6);
        table.setFixedPosition(rootRect.getLeft() + 40, rootRect.getBottom() + 10, rootRect.getWidth() - 80);

        table.addCell(makeFooterCell("Carrefour Kutno", "Oporowska 6A, 99-300 Kutno", "+48 736 810 390"));
        table.addCell(makeFooterCell("Galeria Różana Kutno", "Kościuszki 73, 99-300 Kutno", "+48 579 900 005"));
        table.addCell(makeFooterCell("M Park Piotrków Tryb.", "Sikorskiego 13/17, 97-300 Piotrków", "+48 579 900 070"));
        table.addCell(makeFooterCell("Galeria Zgierska Zgierz", "Armii Krajowej 10, 95-100 Zgierz", "+48 579 900 050"));

        canvas.add(table);
    }

    private Cell makeFooterCell(String title, String address, String tel) {
        return new Cell().setBorder(Border.NO_BORDER)
            .add(new Paragraph(title).setBold())
            .add(new Paragraph(address))
            .add(new Paragraph(tel));
    }

    private void addTableRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value != null && !value.isBlank() ? value : "---")).setBorder(Border.NO_BORDER));
    }
}
