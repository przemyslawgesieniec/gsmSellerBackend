package pl.gesieniec.gsmseller.receipt.handlers;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;

public class HeaderHandler implements IEventHandler {

    private final String logoPath;

    public HeaderHandler(String logoPath) {
        this.logoPath = logoPath;
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdf = docEvent.getDocument();
        PdfPage page = docEvent.getPage();

        Rectangle pageSize = page.getPageSize();

        // Obszar nagłówka
        Rectangle headerArea = new Rectangle(
            pageSize.getLeft() + 40,
            pageSize.getTop() - 70,  // wysokość od góry
            200,
            50
        );

        Canvas canvas = new Canvas(page, headerArea);

        try {
            Image logo = new Image(ImageDataFactory.create(logoPath));
            logo.scaleToFit(120, 50); // dopasuj rozmiar
            canvas.add(logo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        canvas.close();
    }
}

