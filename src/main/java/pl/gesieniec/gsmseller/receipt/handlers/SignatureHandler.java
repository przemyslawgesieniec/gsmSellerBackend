package pl.gesieniec.gsmseller.receipt.handlers;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
public class SignatureHandler implements IEventHandler {

    private final PdfFont font;

    public SignatureHandler(PdfFont font) {
        this.font = font;
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        Rectangle pageSize = page.getPageSize();

        float footerHeight = 70f;
        float signatureHeight = 80f;
        float marginBottom = 20f;

        Rectangle signatureArea = new Rectangle(
            pageSize.getLeft() + 40,
            pageSize.getBottom() + footerHeight + marginBottom,
            pageSize.getWidth() - 80,
            signatureHeight
        );

        Canvas canvas = new Canvas(page, signatureArea);
        canvas.setFont(font); // ⭐ KLUCZOWE ⭐

        float[] columnWidths = {1f, 1f};
        Table table = new Table(columnWidths)
            .setWidth(UnitValue.createPercentValue(100));

        Cell left = new Cell()
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.CENTER)
            .setMinHeight(60);

        left.add(new Paragraph(".............................................").setFontSize(10));
        left.add(new Paragraph("Podpis kupującego").setFontSize(9));

        Cell right = new Cell()
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.CENTER)
            .setMinHeight(60);

        right.add(new Paragraph(".............................................").setFontSize(10));
        right.add(new Paragraph("Podpis i pieczątka sprzedawcy").setFontSize(9));

        table.addCell(left);
        table.addCell(right);

        canvas.add(table);
        canvas.close();
    }
}
