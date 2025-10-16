package pl.gesieniec.gsmseller.receipt;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@Service
public class PdfGenerationService {

    public byte[] generateReceiptPdf(int itemCount) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        // --- Nagłówek dokumentu ---
        document.add(new Paragraph("Potwierdzenie sprzedaży nr 4/07/2020 - oryginał")
            .setBold()
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

        // --- Dane sprzedawcy ---
        document.add(new Paragraph("Sprzedawca:\n24KOM S.C.\nGEODETÓW 2\n05-200 WOŁOMIN\nNIP: 118-220-75-80")
            .setFontSize(10));

        document.add(new Paragraph("\nMiejsce wystawienia: Wołomin" +
            "\nData wystawienia: " + LocalDate.now().withMonth(7).getMonthValue() + "/2020" +
            "\nData sprzedaży: " + LocalDate.now())
            .setFontSize(10)
            .setMarginBottom(15));

        // --- Tabela pozycji ---
        float[] columnWidths = {200f, 50f, 80f, 80f, 80f};
        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(new Cell().add(new Paragraph("Nazwa")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Ilość")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Cena netto")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Wartość netto")).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(new Cell().add(new Paragraph("Wartość brutto")).setBackgroundColor(ColorConstants.LIGHT_GRAY));

        double total = 0.0;
        for (int i = 1; i <= itemCount; i++) {
            double price = 1000 + i * 100;
            total += price;
            table.addCell("SAMSUNG Galaxy A51 (" + i + ")");
            table.addCell("1");
            table.addCell(String.format("%.2f", price));
            table.addCell(String.format("%.2f", price));
            table.addCell(String.format("%.2f", price));
        }

        document.add(table);

        // --- Podsumowanie ---
        document.add(new Paragraph("\nRAZEM: " + String.format("%.2f PLN", total))
            .setBold()
            .setTextAlignment(TextAlignment.RIGHT)
            .setMarginTop(10));

        document.add(new Paragraph("\nForma płatności: Gotówka\nTermin płatności: " + LocalDate.now())
            .setFontSize(10)
            .setMarginTop(10));

        document.close();
        return out.toByteArray();
    }
}
