package pl.gesieniec.gsmseller.receipt;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final PdfGenerationService pdfService;

    public ReceiptController(PdfGenerationService pdfService) {
        this.pdfService = pdfService;
    }

    /**
     * Endpoint: /api/receipts?items=3
     */
    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReceipt(@RequestParam(defaultValue = "1") int items) {
        byte[] pdf = pdfService.generateReceiptPdf(items);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=potwierdzenie_sprzedazy.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
