package pl.gesieniec.gsmseller.receipt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.ItemRequest;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.ReceiptCreateRequest;
import pl.gesieniec.gsmseller.receipt.model.Seller;
import pl.gesieniec.gsmseller.receipt.model.VatRate;

@Slf4j
@Service
@AllArgsConstructor
public class ReceiptService {

    private final PdfGenerationService pdfService;
    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;

    /**
     * Pobiera PDF na podstawie UUID
     */
    public byte[] generateReceiptPdf(UUID technicalId) {

        ReceiptEntity receiptEntity = receiptRepository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Nie można znaleźć potwierdzenia sprzedaży"));

        Receipt receipt = receiptMapper.toModel(receiptEntity);

        log.info("📄 Generowanie PDF dla dokumentu {}", receipt.getNumber());
        return pdfService.generateReceiptPdf(receipt);
    }


    /**
     * Główna logika tworzenia sprzedaży na podstawie danych z frontendu
     */
    @Transactional
    public UUID generateAndSaveReceipt(String username, ReceiptCreateRequest request) {

        log.info("🧾 [{}] Rozpoczynam generowanie dokumentu sprzedaży...", username);

        String receiptNumber = prepareNumber();
        log.info("🧾 Nadano numer dokumentu: {}", receiptNumber);

        VatRate vatRate = VatRate.parse(request.getVatRate());
        log.info("🧾 Stawka VAT dokumentu: {}", vatRate.getName());

        List<Item> items = mapItems(request.getItems(), vatRate);
        log.info("🧾 Dokument zawiera {} pozycji", items.size());

        // TODO: docelowo dane sprzedawcy możesz brać z konfiguracji systemu
        Seller seller = new Seller(
            "Adamowicz group",
            "Aksamitna 123",
            "93-543",
            "Łódź",
            "9467844787"
        );

        Receipt receipt = Receipt.of(
            receiptNumber,
            items,
            seller,
            new DateAndPlace("Łódź", LocalDate.now(), LocalDate.now())
        );

        log.info("🧾 Mapped receipt: {}", receipt);

        ReceiptEntity entity = receiptMapper.toEntity(receipt);
        receiptRepository.save(entity);

        log.info("💾 Zapisano dokument sprzedaży {} przez użytkownika {}", receiptNumber, username);

        return entity.getTechnicalId();
    }


    // ===============================
    // 🔧 MAPPING REQUEST → ITEMS
    // ===============================

    private List<Item> mapItems(List<ItemRequest> itemRequests, VatRate vatRate) {

        return itemRequests.stream()
            .map(req -> {
                if ("PHONE" .equals(req.getItemType())) {
                    log.info("📱 Pozycja PHONE: {}, gwarancja={} mies., używany={}",
                        req.getDescription(), req.getWarrantyMonths(), req.getUsed());

                    return Item.phone(
                        req.getDescription(),
                        req.getPrice(),
                        vatRate,
                        req.getTechnicalId(),
                        req.getWarrantyMonths(),
                        req.getUsed()
                    );
                }

                // MISC
                log.info("📦 Pozycja MISC: {}", req.getDescription());

                return Item.of(req.getDescription(), req.getPrice(), vatRate);

            })
            .collect(Collectors.toList());
    }


    // ===============================
    // 🔧 VAT Mapping
    // ===============================

    private VatRate mapVatRate(String vat) {
        log.info("🔧 mapVatRate() – wejście: {}", vat);

        return switch (vat) {
            case "VAT_23" -> VatRate.VAT_23;
            case "VAT_8" -> VatRate.VAT_8;
            case "VAT_5" -> VatRate.VAT_5;
            case "VAT_0" -> VatRate.VAT_0;
            case "VAT_EXEMPT" -> VatRate.VAT_EXEMPT;
            default -> throw new IllegalArgumentException("Nieznana stawka VAT: " + vat);
        };
    }


    // ===============================
    // 🔢 Numerowanie dokumentów
    // ===============================

    private String prepareNumber() {
        return receiptRepository.getLastReceiptNumber()
            .map(this::incrementInvoiceNumber)
            .orElse("001/" + LocalDate.now().getMonthValue() + "/" + LocalDate.now().getYear());
    }

    private String incrementInvoiceNumber(String lastNumber) {
        log.info("🔢 Ostatni numer dokumentu: {}", lastNumber);

        if (lastNumber == null || lastNumber.isBlank()) {
            throw new IllegalArgumentException("Invalid invoice number: " + lastNumber);
        }

        String[] parts = lastNumber.split("/");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid invoice number: " + lastNumber);
        }

        int n = Integer.parseInt(parts[0]) + 1;

        String newNumber = String.format("%03d", n) + "/" + parts[1] + "/" + parts[2];
        log.info("🔢 Nowy numer dokumentu: {}", newNumber);

        return newNumber;
    }
}
