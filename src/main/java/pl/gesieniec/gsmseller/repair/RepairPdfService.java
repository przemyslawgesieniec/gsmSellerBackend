package pl.gesieniec.gsmseller.repair;

import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.receipt.handlers.FooterHandler;
import pl.gesieniec.gsmseller.receipt.handlers.HeaderHandler;
import pl.gesieniec.gsmseller.receipt.handlers.SignatureHandler;

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
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        PdfFont font = PdfFontFactory.createFont("/fonts/Fira_Sans/FiraSans-Regular.ttf",
            "Identity-H", PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
        document.setFont(font);

        // Stopka
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler(document, font));

        // Nagłówek (logo)
        pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE,
            new HeaderHandler("/imgs/logo_gsm.png"));

        // Tytuł dokumentu
        document.add(new Paragraph(title)
            .setBold()
            .setFontSize(16)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(60)
            .setMarginBottom(20));

        // Data i Miejsce
        document.add(new Paragraph("Data: " + repair.getCreateDateTime().format(DATE_FORMATTER))
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontSize(10)
            .setMarginBottom(20));

        // Dane urządzenia
        document.add(new Paragraph("Dane urządzenia:").setBold().setUnderline().setMarginBottom(10));
        
        float[] colWidths = {150f, 350f};
        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Urządzenie:", repair.getName());
        addTableRow(table, "IMEI:", repair.getImei());
        addTableRow(table, "Kolor:", repair.getColor());
        addTableRow(table, "PIN/Hasło:", repair.getPinPassword());
        
        document.add(table);

        // Opis uszkodzenia i zlecenia
        document.add(new Paragraph("\nOpis uszkodzenia:").setBold().setMarginTop(10));
        document.add(new Paragraph(repair.getDamageDescription() != null ? repair.getDamageDescription() : "---")
            .setFontSize(10)
            .setMarginBottom(10));

        document.add(new Paragraph("Opis zlecenia naprawy:").setBold().setMarginTop(10));
        document.add(new Paragraph(repair.getRepairOrderDescription() != null ? repair.getRepairOrderDescription() : "---")
            .setFontSize(10)
            .setMarginBottom(10));

        // Ceny
        if (repair.getRepairPrice() != null) {
            document.add(new Paragraph("\nPrzewidywany koszt naprawy: " + repair.getRepairPrice() + " zł")
                .setBold()
                .setFontSize(12)
                .setMarginTop(10));
        }

        // Podpisy
        document.setMargins(100, 40, 170, 40);
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new SignatureHandler(font));

        document.close();
        return out.toByteArray();
    }

    private void addTableRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value != null && !value.isBlank() ? value : "---")).setBorder(Border.NO_BORDER));
    }
}
