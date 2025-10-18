package pl.gesieniec.gsmseller.receipt;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE, params = "/{id}")
    public ResponseEntity<byte[]> getReceiptPfd(@PathVariable UUID id) {

        var pdf = receiptService.generateReceiptPdf(id);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=potwierdzenie_sprzedazy.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PostMapping
    public UUID createReceipt() {
        return receiptService.generateAndSaveReceipt();
    }

}
