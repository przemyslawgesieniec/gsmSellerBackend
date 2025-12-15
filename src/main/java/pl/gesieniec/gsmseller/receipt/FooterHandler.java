package pl.gesieniec.gsmseller.receipt;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

public class FooterHandler implements IEventHandler {

    private final Document document;
    private final PdfFont font;

    public FooterHandler(Document document, PdfFont font) {
        this.document = document;
        this.font = font;
    }

    @Override
    public void handleEvent(Event event) {
        PdfPage page = ((PdfDocumentEvent) event).getPage();

        Rectangle pageSize = page.getPageSize();

        float footerHeight = 90;      // wysokość stopki
        float y = pageSize.getBottom() + 20; // odsunięcie od krawędzi

        Rectangle footerArea = new Rectangle(
            pageSize.getLeft() + 40,
            y,
            pageSize.getWidth() - 80,
            footerHeight
        );

        Canvas canvas = new Canvas(page, footerArea);
        canvas.setFont(font);
        canvas.setFontSize(7);

        // Linia oddzielająca
        canvas.add(new Paragraph().setBorderTop(new SolidBorder(1)).setMarginBottom(5));

        float[] colWidth = {1f, 1f, 1f, 1f};
        Table table = new Table(colWidth);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(7);

        table.addCell(makeCell("Carrefour Kutno",
            "Oporowska 6A, 99-300 Kutno",
            "teleakcesoriakutno@gmail.com",
            "+48 736 810 390"));

        table.addCell(makeCell("Galeria Różana Kutno",
            "Tadeusza Kościuszki 73, 99-300 Kutno",
            "teleakcesoriarozana@gmail.com",
            "+48 579 900 005"));

        table.addCell(makeCell("M Park Piotrków Trybunalski",
            "Władysława Sikorskiego 13/17, 97-300 Piotrków Trybunalski",
            "teleakcesoriapiotrkow@gmail.com",
            "+48 579 900 070"));

        table.addCell(makeCell("Galeria Zgierska Zgierz",
            "Armii Krajowej 10, 95-100 Zgierz",
            "teleakcesoriazgierz@gmail.com",
            "+48 579 900 050"));

        canvas.add(table);
        canvas.close();
    }

    private Cell makeCell(String title, String address, String mail, String tel) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER);
        cell.add(new Paragraph(title).setBold());
        cell.add(new Paragraph(address));
        cell.add(new Paragraph(mail));
        cell.add(new Paragraph(tel));
        return cell;
    }
}
