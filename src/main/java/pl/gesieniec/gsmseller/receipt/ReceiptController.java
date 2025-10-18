package pl.gesieniec.gsmseller.receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.Seller;
import pl.gesieniec.gsmseller.receipt.model.VatRate;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final PdfGenerationService pdfService;

    public ReceiptController(PdfGenerationService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReceipt() {

        Receipt receipt = new Receipt("10/10/2025",
            List.of(
                new Item("SAMSUNG GALAXY A64", BigDecimal.valueOf(1234.34), VatRate.VAT_8),
                new Item("IPhone 12 PRO", BigDecimal.valueOf(9034.34), VatRate.VAT_23),
                new Item("Xiaomi Mi7", BigDecimal.valueOf(4), VatRate.VAT_8)
            ),
            new Seller("Adamowicz group", "Aksamitna 123", "93-543", "Łódź", "9467844787"),
            new DateAndPlace("Łódź", LocalDate.now(), LocalDate.now()));

        byte[] pdf = pdfService.generateReceiptPdf(receipt);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=potwierdzenie_sprzedazy.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }


}
