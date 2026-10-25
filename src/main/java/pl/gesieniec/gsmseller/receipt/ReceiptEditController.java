package pl.gesieniec.gsmseller.receipt;

import jakarta.validation.Valid;
import java.util.UUID;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.model.ReceiptEditRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/receipt/{id}/edit")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ReceiptEditController {
    private final ReceiptEditService receiptEditService;

    @GetMapping
    public ReceiptEntity getForEditing(@PathVariable UUID id) {
        return receiptEditService.getForEditing(id);
    }

    @PostMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> editPdf(@PathVariable UUID id,
                                         @Valid @RequestBody ReceiptEditRequest request,
                                         @RequestParam(defaultValue = "false") boolean save) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=faktura.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(receiptEditService.editPdf(id, request, save));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> handleInvalidEdit() {
        return ResponseEntity.badRequest().body(Map.of("message",
            "Sprawdź daty, kwoty (maksymalnie dwa miejsca po przecinku) i stawki VAT"));
    }

}
