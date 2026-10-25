package pl.gesieniec.gsmseller.receipt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.gesieniec.gsmseller.receipt.entity.DateAndPlaceEntity;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptStatus;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.ReceiptEditRequest;
import pl.gesieniec.gsmseller.receipt.model.VatRate;

@Service
@RequiredArgsConstructor
public class ReceiptEditService {
    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;
    private final PdfGenerationService pdfService;

    @Transactional(readOnly = true)
    public ReceiptEntity getForEditing(UUID id) {
        ReceiptEntity entity = findActiveReceipt(id);
        entity.getItems().size(); // Initialize items before leaving the transaction.
        return entity;
    }

    @Transactional
    public byte[] editPdf(UUID id, ReceiptEditRequest request, boolean save) {
        ReceiptEntity entity = findActiveReceipt(id);
        Map<Long, ReceiptEditRequest.EditedItem> changes = new HashMap<>();
        request.items().forEach(item -> {
            if (changes.put(item.id(), item) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Powtórzona pozycja faktury");
            }
        });
        if (changes.size() != entity.getItems().size()
            || entity.getItems().stream().anyMatch(item -> !changes.containsKey(item.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prześlij wszystkie pozycje tej faktury");
        }

        // Build a separate PDF model: downloading alone must never dirty managed entities.
        List<Item> editedItems = entity.getItems().stream().map(item -> {
            var change = changes.get(item.getId());
            return new Item(item.getName(), change.nettAmount(), item.getVatRate(),
                item.getVatAmount(), item.getGrossAmount(), item.getTechnicalId(),
                item.getWarrantyMonths(), item.getUsed(), item.getItemType(), item.getSellingInfo())
                .withVat(VatRate.parse(change.vatRate()));
        }).toList();
        Receipt original = receiptMapper.toModel(entity);
        Receipt edited = new Receipt(original.getNumber(), original.getTechnicalId(), original.getStatus(),
            editedItems, original.getSeller(),
            new DateAndPlace(original.getDateAndPlace().place(), request.generateDate(), request.sellDate()),
            original.getCreatedBy(), original.getCustomerNote());
        byte[] pdf = pdfService.generateReceiptPdf(edited);

        if (save) {
            entity.setDateAndPlace(new DateAndPlaceEntity(entity.getDateAndPlace().getPlace(),
                request.generateDate(), request.sellDate()));
            for (int i = 0; i < entity.getItems().size(); i++) {
                entity.getItems().get(i).updateAmounts(editedItems.get(i));
            }
            receiptRepository.save(entity);
        }
        return pdf;
    }

    private ReceiptEntity findActiveReceipt(UUID id) {
        ReceiptEntity entity = receiptRepository.findByTechnicalId(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono faktury"));
        if (entity.getStatus() != ReceiptStatus.AKTYWNA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nie można poprawić wycofanej faktury");
        }
        return entity;
    }
}
