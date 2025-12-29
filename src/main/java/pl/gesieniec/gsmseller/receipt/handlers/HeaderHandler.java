package pl.gesieniec.gsmseller.receipt.handlers;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import java.io.InputStream;

public class HeaderHandler implements IEventHandler {

    private final String logoPath;

    public HeaderHandler(String logoPath) {
        this.logoPath = logoPath;
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        Rectangle pageSize = page.getPageSize();

        Rectangle headerArea = new Rectangle(
            pageSize.getLeft() + 40,
            pageSize.getTop() - 70,
            200,
            50
        );

        try (Canvas canvas = new Canvas(page, headerArea)) {

            InputStream is = getClass().getResourceAsStream(logoPath);
            if (is == null) {
                throw new RuntimeException("Nie znaleziono zasobu: " + logoPath);
            }

            ImageData imageData = ImageDataFactory.create(is.readAllBytes());
            Image logo = new Image(imageData);
            logo.scaleToFit(120, 50);

            canvas.add(logo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
