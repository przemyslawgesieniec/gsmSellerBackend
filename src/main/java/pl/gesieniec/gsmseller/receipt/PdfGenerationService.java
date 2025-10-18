package pl.gesieniec.gsmseller.receipt;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.util.List;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.Seller;

@Service
public class PdfGenerationService {

    public byte[] generateReceiptPdf(Receipt receipt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        prepareHeader(document, receipt);

        prepareTopPart(receipt, document);

        prepareItemsTable(document, receipt.getItems());

        prepareSummary(document, receipt);

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
        table.addCell(receipt.getItems().getFirst().getVat().getName());
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
            table.addCell(item.getName());
            table.addCell("1")
                .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(String.format("%.2f",item.getNettAmount()))
                .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(String.format("%.2f",item.getNettAmount()))
                .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(item.getVat().getName())
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
            .setMarginBottom(40));
    }

}
