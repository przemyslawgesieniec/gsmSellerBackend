package pl.gesieniec.gsmseller.receipt;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import pl.gesieniec.gsmseller.common.ItemType;
import pl.gesieniec.gsmseller.receipt.handlers.FooterHandler;
import pl.gesieniec.gsmseller.receipt.handlers.HeaderHandler;
import pl.gesieniec.gsmseller.receipt.handlers.SignatureHandler;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.Seller;

@Service
public class PdfGenerationService {

    @SneakyThrows
    public byte[] generateReceiptPdf(Receipt receipt) {
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

        prepareHeader(document, receipt);

        prepareTopPart(receipt, document);

        prepareItemsTable(document, receipt.getItems());

        prepareSummary(document, receipt);

        prepareRemarks(document, receipt);
        document.setMargins(100, 40, 170, 40);

        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new SignatureHandler());

        document.close();
        return out.toByteArray();
    }


    private void prepareTopPart(Receipt receipt, Document document) {
        float[] columnWidths = {100f, 100f};
        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        Cell leftCell = prepareSeller(receipt.getSeller());
        Cell rightCell = preparePlaceAndDate(receipt.getDateAndPlace());

        table.addCell(leftCell);
        table.addCell(rightCell);
        table.setMarginBottom(60);
        document.add(table);
    }

    private void prepareSummary(Document document, Receipt receipt) {
        float[] columnWidths = {100f, 120f, 120f, 120f};
        Table table = new Table(columnWidths);
        table.setFontSize(8);
        table.setWidth(UnitValue.createPercentValue(50));

        table.addHeaderCell(new Cell().add(new Paragraph(""))
            .setBold()
            .setTextAlignment(TextAlignment.RIGHT)
            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Obrót netto"))
            .setBold()
            .setTextAlignment(TextAlignment.RIGHT)
            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell()
            .setBold()
            .setTextAlignment(TextAlignment.RIGHT)
            .add(new Paragraph("Stawka VAT (PLN)")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell()
            .setBold()
            .setTextAlignment(TextAlignment.RIGHT)
            .add(new Paragraph("Obrót brutto (PLN)")).setBackgroundColor(ColorConstants.LIGHT_GRAY));

        table.addCell("RAZEM: ");
        table.addCell(String.format("%.2f", receipt.getNetTotal()));
        table.addCell(receipt.getItems().get(0).getVatRate().getName());
        table.addCell(String.format("%.2f", receipt.getGrossTotal()));

        table.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        document.add(table);
    }

    private void prepareItemsTable(Document document, List<Item> items) {
        float[] columnWidths = {200f, 30f, 80f, 80f, 80f, 80f, 80f};
        Table table = new Table(columnWidths);
        table.setFontSize(8);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(new Cell().add(new Paragraph("Nazwa")).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.RIGHT));
        table.addHeaderCell(new Cell().add(new Paragraph("Ilość")).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell().add(new Paragraph("Cena netto (PLN)")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Wartość netto (PLN)")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Stawka VAT")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Wartość VAT (PLN)")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Wartość brutto (PLN)")).setBackgroundColor(ColorConstants.LIGHT_GRAY));

        for (Item item : items) {
            table.addCell(item.getName() + "\n" + item.getWarrantyMonthsFormatted());
            table.addCell("1")
                .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(String.format("%.2f",item.getNettAmount()))
                .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(String.format("%.2f",item.getNettAmount()))
                .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(item.getVatRate().getName())
                .setTextAlignment(TextAlignment.CENTER);
            table.addCell(String.format("%.2f",item.getVatAmount()))
                .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(String.format("%.2f",item.getGrossAmount()))
                .setTextAlignment(TextAlignment.RIGHT);
        }
        table.setMarginBottom(40);
        document.add(table);
    }

    private Cell preparePlaceAndDate(DateAndPlace dateAndPlace) {
        return new Cell()
            .add(new Paragraph(
                    "\nMiejsce wystawienia: " + dateAndPlace.place() +
                        "\nData wystawienia: " + dateAndPlace.generateDate() +
                        "\nData sprzedaży: " + dateAndPlace.sellDate())
                    .setFontSize(10)
                    .setMarginBottom(15))
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(Border.NO_BORDER);
    }

    private Cell prepareSeller(Seller seller) {
        return new Cell()
            .add(new Paragraph("Sprzedawca: ").setTextAlignment(TextAlignment.LEFT))
            .add(new Paragraph(
                seller.getName() + "\n" +
                    seller.getStreet() + "\n" +
                    seller.getCity() + " " + seller.getPostalCode() + "\n" +
                    "NIP: " + seller.getNip())
                .setFontSize(10).setBold()).setTextAlignment(TextAlignment.LEFT)
            .setBorder(Border.NO_BORDER);
    }

    private void prepareHeader(Document document, Receipt receipt) {
        document.add(new Paragraph("Potwierdzenie sprzedaży nr " + receipt.getNumber() + " - oryginał")
            .setBold()
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(40)
            .setMarginBottom(40));
    }


    private void prepareRemarks(Document document, Receipt receipt) {
        boolean hasUsedItem = receipt.getItems()
            .stream()
            .filter(e->e.getItemType().equals(ItemType.PHONE))
            .anyMatch(Item::getUsed);

        if (!hasUsedItem) {
            return;
        }

        // Ramka po prawej stronie
        float[] columnWidths = {350f, 200f}; // lewa część pusta, prawa to ramka
        Table wrapper = new Table(columnWidths);
        wrapper.setWidth(UnitValue.createPercentValue(100));

        // Lewa pusta komórka
        wrapper.addCell(new Cell()
            .setBorder(Border.NO_BORDER));

        // Prawa komórka z ramką i treścią
        Cell remarksCell = new Cell()
            .add(new Paragraph("Uwagi").setBold().setFontSize(10))
            .add(new Paragraph(
                "Gwarancja j/w z wyłączeniem rękojmi sprzedawcy ze względu na niższą " +
                    "cenę towaru niż rynkowa na dzień sprzedaży urządzenia. Zwrot towaru niemożliwy.")
                .setFontSize(9)
                .setMarginTop(5))
            .setBorder(new SolidBorder(1))
            .setPadding(10)
            .setTextAlignment(TextAlignment.LEFT);

        wrapper.addCell(remarksCell);

        wrapper.setMarginTop(20);
        wrapper.setHorizontalAlignment(HorizontalAlignment.RIGHT);

        document.add(wrapper);
    }

}
