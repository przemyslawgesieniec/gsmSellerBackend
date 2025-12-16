package pl.gesieniec.gsmseller.receipt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;
import pl.gesieniec.gsmseller.common.ItemType;
import pl.gesieniec.gsmseller.event.ItemsSoldEvent;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.ItemRequest;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.ReceiptCreateRequest;
import pl.gesieniec.gsmseller.receipt.model.Seller;
import pl.gesieniec.gsmseller.receipt.model.VatRate;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserService;

@Slf4j
@Service
@AllArgsConstructor
public class ReceiptService {

    private final PdfGenerationService pdfService;
    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;

    public Page<ReceiptEntity> getReceipts(String username, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createDate"));

        return receiptRepository.findByCreatedByOrderByCreateDateDesc(username, pageable);
    }


    public byte[] generateReceiptPdf(UUID technicalId) {

        ReceiptEntity receiptEntity = receiptRepository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Nie można znaleźć potwierdzenia sprzedaży"));

        Receipt receipt = receiptMapper.toModel(receiptEntity);

        log.info("📄 Generowanie PDF dla dokumentu {}", receipt.getNumber());
        return pdfService.generateReceiptPdf(receipt);
    }


    @Transactional
    public UUID generateAndSaveReceipt(String username, ReceiptCreateRequest request) {
        Receipt receipt = generateReceipt(username, request);

        ReceiptEntity entity = receiptMapper.toEntity(receipt);
        receiptRepository.save(entity);

        eventPublisher.publishEvent(new ItemsSoldEvent(username, receipt.getNumber(), receipt.getItems()));
        log.info("💾 Zapisano dokument sprzedaży {} przez użytkownika {}", receipt.getNumber(), username);

        return entity.getTechnicalId();
    }

    public Receipt generateReceipt(String username, ReceiptCreateRequest request) {
        Optional<User> userByUsername = userService.getUserByUsername(username);

        log.info("🧾 [{}] Rozpoczynam generowanie dokumentu sprzedaży...", username);

        LocalDate sellDate = request.getSellDate() != null
            ? request.getSellDate()
            : LocalDate.now();

        String receiptNumber = prepareNumber(sellDate);
        log.info("🧾 Nadano numer dokumentu: {}", receiptNumber);

        VatRate vatRate = VatRate.parse(request.getVatRate());
        log.info("🧾 Stawka VAT dokumentu: {}", vatRate.getName());

        List<Item> items = mapItems(request.getItems(), vatRate);
        log.info("🧾 Dokument zawiera {} pozycji", items.size());

        // TODO: docelowo dane sprzedawcy możesz brać z konfiguracji systemu
        Seller seller = new Seller(
            "Teleakcesoria Paweł Jarocki",
            "Ul.Krótka 5A",
            "95-010",
            "Stryków",
            "7331320587"
        );

        Receipt receipt = Receipt.of(
            receiptNumber,
            items,
            seller,
            new DateAndPlace(userByUsername
                .map(e -> e.getLocation().getCity())
                .orElse("Stryków"),
                sellDate, sellDate),
            username
        );

        log.info("🧾 Mapped receipt: {}", receipt);

        return receipt;
    }


    // ===============================
    // 🔧 MAPPING REQUEST → ITEMS
    // ===============================

    private List<Item> mapItems(List<ItemRequest> itemRequests, VatRate vatRate) {

        return itemRequests.stream()
            .map(req -> {
                if (ItemType.PHONE.equals(req.getItemType())) {
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
    // 🔢 Numerowanie dokumentów
    // ===============================

    private String prepareNumber(LocalDate sellDate) {
        return receiptRepository.findFirstByOrderByCreateDateDesc()
            .map(e -> incrementInvoiceNumber(e.getNumber()))
            .orElse("001/" + sellDate.getMonthValue() + "/" + sellDate.getYear());
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

    @Transactional(readOnly = true)
    public byte[] buildServiceInvoice(UUID receiptTechnicalId) {

        // 1️⃣ Pobranie oryginalnego dokumentu
        ReceiptEntity entity = receiptRepository
            .findByTechnicalId(receiptTechnicalId)
            .orElseThrow(() ->
                new IllegalArgumentException("Nie znaleziono dokumentu sprzedaży")
            );

        // 2️⃣ Mapowanie encji → model PDF (ORYGINAŁ)
        Receipt originalReceipt = new Receipt(
            entity.getNumber(),
            entity.getTechnicalId(),
            entity.getItems().stream()
                .map(receiptMapper::toModel)
                .toList(),
            receiptMapper.toModel(entity.getSeller()),
            receiptMapper.toModel(entity.getDateAndPlace()),
            entity.getCreatedBy()
        );

        Receipt serviceReceipt = originalReceipt.withVat(VatRate.VAT_23);

        return pdfService.generateReceiptPdf(serviceReceipt);
    }

}
