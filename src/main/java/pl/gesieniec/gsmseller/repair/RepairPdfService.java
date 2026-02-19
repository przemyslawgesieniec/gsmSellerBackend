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
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.location.LocationRepository;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

@Service
@RequiredArgsConstructor
public class RepairPdfService {

    private final LocationRepository locationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @SneakyThrows
    public byte[] generateRepairReceiptPdf(Repair repair) {
        return generatePdf(repair, "POKWITOWANIE PRZYJĘCIA TELEFONU NA SERWIS");
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
        float marginH = 50f;
        float marginV = 20f;

        // Egzemplarz 1 (Góra)
        Rectangle rectUpper =
            new Rectangle(marginH, marginV, pageSize.getWidth() - 2 * marginH, pageSize.getHeight() - 2 * marginV);
        Canvas canvasUpper = new Canvas(page, rectUpper);
        drawContent(canvasUpper, repair, title, font, rectUpper, true);
        canvasUpper.close();

        pdfDoc.close();
        return out.toByteArray();
    }

    @SneakyThrows
    private void drawContent(Canvas canvas, Repair repair, String title, PdfFont font, Rectangle rootRect,
                             boolean withLegalNote) {
        canvas.setFont(font);

        // Logo
        drawLogo(canvas, rootRect);

        //data, RMA, kontakt
        rightTopCorner(canvas, repair);

        // Tytuł dokumentu
        canvas.add(new Paragraph(title)
            .setBold()
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20));


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
            String description = repair.getRepairDescription();
            if (description == null || description.isBlank()) {
                description = repair.getRepairOrderDescription() != null ? repair.getRepairOrderDescription() : "---";
            }
            canvas.add(new Paragraph(description).setFontSize(9));
            if (repair.getRepairPrice() != null) {
                canvas.add(new Paragraph("Koszt naprawy: " + repair.getRepairPrice() + " zł")
                    .setBold().setFontSize(10));
            }
            canvas.add(new Paragraph("\nPotwierdzam odbiór sprawnego urządzenia.")
                .setBold().setFontSize(9).setItalic());
        } else if (repair.getStatus() == RepairStatus.ANULOWANY) {
            canvas.add(new Paragraph("\nStatus: ANULOWANO").setBold().setFontSize(10));
            if (repair.getRepairDescription() != null && !repair.getRepairDescription().isBlank()) {
                canvas.add(new Paragraph("Opis: " + repair.getRepairDescription()).setFontSize(9));
            } else {
                canvas.add(new Paragraph("Naprawa została anulowana na prośbę klienta lub z przyczyn technicznych.")
                        .setFontSize(9));
            }
            canvas.add(new Paragraph("\nPotwierdzam odbiór nienaprawionego urządzenia.")
                .setBold().setFontSize(9).setItalic());
        } else if (repair.getStatus() == RepairStatus.NIE_DO_NAPRAWY) {
            canvas.add(new Paragraph("\nStatus: NIE DO NAPRAWY").setBold().setFontSize(10));
            if (repair.getRepairDescription() != null && !repair.getRepairDescription().isBlank()) {
                canvas.add(new Paragraph("Opis: " + repair.getRepairDescription()).setFontSize(9));
            } else {
                canvas.add(new Paragraph("Urządzenie zostało sprawdzone, jednak naprawa jest niemożliwa lub nieopłacalna.")
                        .setFontSize(9));
            }
            canvas.add(new Paragraph("\nPotwierdzam odbiór nienaprawionego urządzenia.")
                .setBold().setFontSize(9).setItalic());
        } else {
            // Domyślnie (np. DO_NAPRAWY - pokwitowanie przyjęcia)
            canvas.add(new Paragraph("\nOpis uszkodzenia:").setBold().setFontSize(10));
            canvas.add(new Paragraph(repair.getDamageDescription() != null ? repair.getDamageDescription() : "---")
                .setFontSize(9));

            Paragraph costParagraph = new Paragraph("Przewidywany koszt: " +
                (repair.getEstimatedCost() != null ? repair.getEstimatedCost() + " zł" : "do ustalenia"))
                .setFontSize(10).setBold();
            if (repair.getAdvancePayment() != null && repair.getAdvancePayment().compareTo(BigDecimal.ZERO) > 0) {
                costParagraph.add(new Paragraph(" | Zaliczka: " + repair.getAdvancePayment() + " zł").setBold());
            }
            canvas.add(costParagraph);
        }

        // Notka prawna
        if (withLegalNote) {
            canvas.add(new Paragraph(
                "Informujemy że nie ponosimy odpowiedzialności za pozostawione karty sim i karty pamięci w serwisie gsm. " +
                    "Serwis nie odpowiada za ukryte wady urządzenia, których nie stwierdzono przy przyjęciu sprzętu do naprawy. " +
                    "Na urządzenie zawilgocone i/lub po ingerencji osób trzecich serwis nie udziela gwarancji na naprawę. " +
                    "Informuje się , że nie dokonanie odbioru telefonu po 3 miesiącach (90 dni) od momentu poinformowania klienta o możliwości odbioru skutkuje przejęciem telefonu na własność serwisu. " +
                    "Serwis nie ponosi odpowiedzialności za utratę danych z telefonu , ani dalszych strat , które w wyniku tego mogą nastąpić " +
                    "Wyrażam zgodę na przetwarzania moich danych osobowych zgodnie z rozporządzeniem o ochronie danych z dnia 27 kwietnia 2016 r., tzw. rozporządzeniem RODO przez firmę Teleakcesoria Paweł Jarocki z siedzibą w Stryków ul. Krótka 5A w celu wykonania zleconych uslug. " +
                    "Oświadczam, że firma Teleakcesoria Paweł Jarocki poinformowała mnie o dobrowolności podania danych, przysługujących mi prawach w szczególności o prawie dostępu do treści danych ,ich poprawiania i usuwania. " +
                    "Wyrażam równocześnie zgodę na otrzymywanie od firmy Teleakcesoria Paweł Jarocki informacji dotyczących zleconych usług za pomocą środków komunikacji elektronicznej oraz oświadczam, ze zapoznałem(łam) sie z regulaminem serwisu." +
                    "Administratorem Twoich danych osobowych jest Teleakcesoria Paweł Jarocki z siedzibą w Stryków ul. Krótka 5A (Administrator Danych Osobowych/ADO). " +
                    "We wszystkich kwestiach związanych z przetwarzaniem Twoich danych osobowych możesz się skontaktować z ADO poprzez adres poczty elektronicznej teleakcesoriamorena@gmail.com.")
                .setFontSize(6)
                .setFontColor(ColorConstants.GRAY)
                .setMarginTop(10));
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
