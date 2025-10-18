package pl.gesieniec.gsmseller.receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.common.EntityNotFountException;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.Seller;
import pl.gesieniec.gsmseller.receipt.model.VatRate;


@Slf4j
@Service
@AllArgsConstructor
public class ReceiptService {

    private final PdfGenerationService pdfService;
    private final ReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;

    public byte[] generateReceiptPdf(UUID technicalId) {

        ReceiptEntity receiptEntity = receiptRepository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFountException("Nie można znaleść potwierdzenia sprzedaży"));

        Receipt receipt = receiptMapper.toModel(receiptEntity);
        return pdfService.generateReceiptPdf(receipt);
    }

    public UUID generateAndSaveReceipt() {
        Receipt receipt = Receipt.of("10/10/2025",
            List.of(
                Item.of("SAMSUNG GALAXY A64", BigDecimal.valueOf(1234.34), VatRate.VAT_8),
                Item.of("IPhone 12 PRO", BigDecimal.valueOf(9034.34), VatRate.VAT_23),
                Item.of("Xiaomi Mi7", BigDecimal.valueOf(4), VatRate.VAT_8)
            ),
            new Seller("Adamowicz group", "Aksamitna 123", "93-543", "Łódź", "9467844787"),
            new DateAndPlace("Łódź", LocalDate.now(), LocalDate.now()));

        ReceiptEntity receiptEntity = receiptMapper.toEntity(receipt);
        receiptRepository.save(receiptEntity);

        log.info("Receipt {} generated and saved", receiptEntity);
        return receiptEntity.getTechnicalId();
    }
}
