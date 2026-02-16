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
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.location.LocationRepository;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

@Service
@RequiredArgsConstructor
public class RepairHandoverPdfService {

    private final LocationRepository locationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @SneakyThrows
    public byte[] generateRepairHandoverPdf(Repair repair) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);

        PdfFont font = PdfFontFactory.createFont("/fonts/Fira_Sans/FiraSans-Regular.ttf",
            "Identity-H", PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);

        PdfPage page = pdfDoc.addNewPage(PageSize.A4);
        Rectangle pageSize = page.getPageSize();
        float marginH = 50f;
        float marginV = 20f;
        float middleY = pageSize.getHeight() / 2;

        // Egzemplarz 1 (Góra)
        Rectangle rectUpper =
            new Rectangle(marginH, middleY + marginV, pageSize.getWidth() - 2 * marginH, middleY - 2 * marginV);
        Canvas canvasUpper = new Canvas(page, rectUpper);
        canvasUpper.setFont(font);
        drawContent(canvasUpper, repair, "POTWIERDZENIE ODBIORU URZĄDZENIA Z SERWISU", rectUpper);
        canvasUpper.close();

        // Linia przerywana
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        pdfCanvas.setLineDash(3, 3);
        pdfCanvas.moveTo(0, middleY);
        pdfCanvas.lineTo(pageSize.getWidth(), middleY);
        pdfCanvas.stroke();

        // Egzemplarz 2 (Dół)
        Rectangle rectLower = new Rectangle(marginH, marginV, pageSize.getWidth() - 2 * marginH, middleY - 2 * marginV);
        Canvas canvasLower = new Canvas(page, rectLower);
        drawContent(canvasLower, repair, "POTWIERDZENIE ODBIORU URZĄDZENIA Z SERWISU", rectLower);
        canvasLower.close();

        pdfDoc.close();
        return out.toByteArray();
    }

    @SneakyThrows
    private void drawContent(Canvas canvas, Repair repair, String title, Rectangle rootRect) {

        // Logo
        drawLogo(canvas, rootRect);

        //data, RMA, kontakt
        rightTopCorner(canvas, repair);

        // Tytuł dokumentu
        canvas.add(new Paragraph(title)
            .setBold()
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10));


        // Dane urządzenia
        canvas.add(new Paragraph("Dane urządzenia:").setBold().setUnderline().setFontSize(10).setMarginTop(5));

        float[] colWidths = {120f, 300f};
        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(9);

        addTableRow(table, "Urządzenie:", (repair.getDeviceType() != null ? repair.getDeviceType() + " " : "") +
            (repair.getManufacturer() != null ? repair.getManufacturer() + " " : "") +
            (repair.getModel() != null ? repair.getModel() : ""));
        addTableRow(table, "IMEI:", repair.getImei());

        if (repair.getClient() != null) {
            addTableRow(table, "Klient:", repair.getClient().getName() + " " + repair.getClient().getSurname());
            addTableRow(table, "Tel. klienta:", repair.getClient().getPhoneNumber());
        }

        canvas.add(table);

        // Status-specific content
        if (repair.getStatus() == RepairStatus.NAPRAWIONY) {
            canvas.add(new Paragraph("\nOpis naprawy:").setBold().setFontSize(10));
            canvas.add(
                new Paragraph(repair.getRepairOrderDescription() != null ? repair.getRepairOrderDescription() : "---")
                    .setFontSize(9));
            if (repair.getRepairPrice() != null) {
                canvas.add(new Paragraph("Koszt naprawy: " + repair.getRepairPrice() + " zł")
                    .setBold().setFontSize(10));
            }
            canvas.add(new Paragraph("\nPotwierdzam odbiór sprawnego urządzenia.")
                .setBold().setFontSize(9).setItalic());
        } else if (repair.getStatus() == RepairStatus.ANULOWANY) {
            canvas.add(new Paragraph("\nStatus: ANULOWANO").setBold().setFontSize(10));
            canvas.add(new Paragraph("Naprawa została anulowana na prośbę klienta lub z przyczyn technicznych.")
                .setFontSize(9));
            canvas.add(new Paragraph("\nPotwierdzam odbiór nienaprawionego urządzenia.")
                .setBold().setFontSize(9).setItalic());
        } else if (repair.getStatus() == RepairStatus.NIE_DO_NAPRAWY) {
            canvas.add(new Paragraph("\nStatus: NIE DO NAPRAWY").setBold().setFontSize(10));
            canvas.add(new Paragraph("Urządzenie zostało sprawdzone, jednak naprawa jest niemożliwa lub nieopłacalna.")
                .setFontSize(9));
            canvas.add(new Paragraph("\nPotwierdzam odbiór nienaprawionego urządzenia.")
                .setBold().setFontSize(9).setItalic());
        }

        // Podpisy
        drawSignatures(canvas, rootRect);

        // Stopka
        drawFooter(canvas, rootRect);
    }

    private void rightTopCorner(Canvas canvas, Repair repair) {
        // Data
        canvas.add(new Paragraph("Data: " + repair.getCreateDateTime().format(DATE_FORMATTER))
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontSize(9));

        locationRepository.findByName(repair.getLocation())
            .map(LocationEntity::getPhoneNumber)
            .ifPresent(phoneNumber -> {
                canvas.add(new Paragraph("kontakt: " + phoneNumber)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontSize(9));
            });


        if (repair.getBusinessId() != null) {
            canvas.add(new Paragraph(repair.getBusinessId())
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(9));
        }
    }

    @SneakyThrows
    private void drawLogo(Canvas canvas, Rectangle rootRect) {
        try (InputStream is = getClass().getResourceAsStream("/imgs/logo_gsm.png")) {
            if (is != null) {
                ImageData imageData = ImageDataFactory.create(is.readAllBytes());
                Image logo = new Image(imageData);
                logo.scaleToFit(100, 40);
                logo.setFixedPosition(rootRect.getLeft(), rootRect.getTop() - 40);
                canvas.add(logo);
            }
        }
    }

    private void drawSignatures(Canvas canvas, Rectangle rootRect) {
        Table signatureTable = new Table(new float[] {1, 1});
        signatureTable.setWidth(UnitValue.createPercentValue(100));
        
        // Ustawienie podpisów powyżej stopki (stopka zaczyna się na 45 od dołu prostokąta)
        signatureTable.setFixedPosition(rootRect.getLeft(), rootRect.getBottom() + 65, rootRect.getWidth());

        signatureTable.addCell(new Cell().add(new Paragraph("........................................\nPodpis klienta")
            .setTextAlignment(TextAlignment.CENTER).setFontSize(8)).setBorder(Border.NO_BORDER));
        signatureTable.addCell(
            new Cell().add(new Paragraph("........................................\nPieczątka i podpis serwisu")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(8)).setBorder(Border.NO_BORDER));
        
        canvas.add(signatureTable);
    }

    private void drawFooter(Canvas canvas, Rectangle rootRect) {
        Paragraph footerLine = new Paragraph().setBorderTop(new SolidBorder(ColorConstants.BLACK, 0.5f));
        footerLine.setFixedPosition(rootRect.getLeft(), rootRect.getBottom() + 45, rootRect.getWidth());
        canvas.add(footerLine);

        Table table = new Table(new float[] {1, 1, 1, 1});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(6);
        table.setFixedPosition(rootRect.getLeft(), rootRect.getBottom(), rootRect.getWidth());

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
        table.addCell(new Cell().add(new Paragraph(value != null && !value.isBlank() ? value : "---"))
            .setBorder(Border.NO_BORDER));
    }
}
