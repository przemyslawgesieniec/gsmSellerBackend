package pl.gesieniec.gsmseller.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException e) {
        return e.getMessage();
    }

    @GetMapping
    public List<PurchaseResponse> getPurchases() {
        return purchaseService.getAllPurchases().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @PostMapping("/{technicalId}/close")
    public void closePurchase(
            @PathVariable UUID technicalId,
            @RequestBody ClosePurchaseRequest request
    ) {
        log.info("Closing purchase: {} with reason: {} and contacted: {}", technicalId, request.reason(), request.contactedCustomer());
        purchaseService.closePurchase(technicalId, request.reason(), request.contactedCustomer());
    }

    @GetMapping("/photos/{photoTechnicalId}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID photoTechnicalId) {
        PurchasePhoto photo = purchaseService.getPhoto(photoTechnicalId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(photo.getData());
    }

    private PurchaseResponse mapToResponse(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getTechnicalId(),
                purchase.getPhoneModel(),
                purchase.getPhoneNumber(),
                purchase.getDescription(),
                purchase.getStatus(),
                purchase.getCreatedAt(),
                purchase.getPhotos().stream().map(PurchasePhoto::getTechnicalId).toList(),
                purchase.getClosureReason(),
                purchase.getContactedCustomer()
        );
    }

    public record ClosePurchaseRequest(String reason, boolean contactedCustomer) {}

    public record PurchaseResponse(
            UUID technicalId,
            String phoneModel,
            String phoneNumber,
            String description,
            PurchaseStatus status,
            java.time.LocalDateTime createdAt,
            List<UUID> photoIds,
            String closureReason,
            Boolean contactedCustomer
    ) {}
}
