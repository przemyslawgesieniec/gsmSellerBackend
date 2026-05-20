package pl.gesieniec.gsmseller.receipt;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.ReceiptCreateRequest;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/receipt")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/list")
    public Page<ReceiptEntity> listReceipts(
        Principal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(required = false) String imei,
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No principal");
        }

        String username = principal.getName();

        log.info(
            "📄 [GET RECEIPTS LIST] user={} page={} size={} imei={} invoiceNumber={} dateFrom={} dateTo={}",
            username, page, size, imei, invoiceNumber, dateFrom, dateTo
        );

        return receiptService.getReceipts(username, page, size, imei, invoiceNumber, dateFrom, dateTo);
    }


    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE, path = "/{id}")
    public ResponseEntity<byte[]> getReceiptPfd(@PathVariable UUID id) {

        log.info("📄 [GET RECEIPT PDF] Wejście do endpointu z id={}", id);

        byte[] pdf;
        try {
            pdf = receiptService.generateReceiptPdf(id);
        } catch (Exception e) {
            log.error("❌ [GET RECEIPT PDF] Błąd podczas generowania PDF dla id={}", id, e);
            throw e;
        }

        int size = (pdf != null) ? pdf.length : 0;
        log.info("📄 [GET RECEIPT PDF] Wygenerowano PDF dla id={}, rozmiar={} bajtów", id, size);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=potwierdzenie_sprzedazy.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PostMapping
    @Transactional
    public UUID createReceipt(Principal principal,
                              @Valid @RequestBody ReceiptCreateRequest request) {

        log.info("🧾 [CREATE RECEIPT] Wejście do endpointu createReceipt");

        if (principal == null) {
            log.warn("🧾 [CREATE RECEIPT] Brak principal – UNAUTHORIZED");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No principal");
        }

        String username = principal.getName();
        log.info("🧾 [CREATE RECEIPT] Użytkownik={} przesyła request: {}", username, request);

        UUID receiptId;
        try {
            receiptId = receiptService.generateAndSaveReceipt(username, request);
        } catch (Exception e) {
            log.error("❌ [CREATE RECEIPT] Błąd podczas generowania paragonu dla użytkownika={}", username, e);
            throw e;
        }

        log.info("🧾 [CREATE RECEIPT] Utworzono dokument sprzedaży: id={} dla user={}", receiptId, username);

        return receiptId;
    }

    @GetMapping(
        path = "/{id}/service-invoice",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> getServiceInvoicePdf(@PathVariable UUID id) {

        log.info("🧾 [GET SERVICE INVOICE PDF] id={}", id);

        byte[] pdf = receiptService.buildServiceInvoice(id);

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=faktura_serwisowa.pdf"
            )
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public void cancelReceipt(
        @PathVariable UUID id,
        Principal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        log.warn("🛑 [CANCEL RECEIPT] id={} user={}", id, principal.getName());

        receiptService.cancelReceipt(id, principal.getName());
    }


}
