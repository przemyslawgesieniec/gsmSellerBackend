package pl.gesieniec.gsmseller.phone.scan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.phone.scan.parser.OcrDataParser;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockService;
import pl.gesieniec.gsmseller.phone.stock.model.PurchaseType;

@Slf4j
@Service
@AllArgsConstructor
public class PhoneScanService {

    private final GoogleCloudVision googleCloudVision;
    private final OcrDataParser phoneDataOcrParser;
    private final PhoneStockService phoneStockService;
    private final ExecutorService ocrExecutor;

    public List<PhoneScanDto> getPhoneScanDtos(
        String name,
        String source,
        String initialPrice,
        String sellingPrice,
        List<MultipartFile> photos,
        PurchaseType purchaseType,
        String description,
        String batteryCondition,
        boolean used
    ) {

        log.info("📦 Phone scan started | photos={}", photos != null ? photos.size() : 0);

        assert photos != null;
        List<CompletableFuture<PhoneScanDto>> futures =
            photos.stream()
                .map(photo ->
                    CompletableFuture.supplyAsync(
                            () -> processPhoto(photo,
                name,
                source,
                initialPrice,
                sellingPrice,
                purchaseType,
                description,
                batteryCondition,
                used
            ),ocrExecutor))
            .toList();

        List<PhoneScanDto> scanned = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        // 🔹 deduplikacja IMEI w request
        Map<String, PhoneScanDto> uniqueByImei = scanned.stream()
            .filter(dto -> dto.getImei() != null)
            .collect(Collectors.toMap(
                PhoneScanDto::getImei,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
            ));

        // 🔹 sprawdzenie duplikatów w DB jednym strzałem
        List<String> imeis = new ArrayList<>(uniqueByImei.keySet());
        var existingImeis = phoneStockService.findActiveImeis(imeis);

        return uniqueByImei.values().stream()
            .filter(dto -> !existingImeis.contains(dto.getImei()))
            .collect(Collectors.toList());
    }

    private PhoneScanDto processPhoto(
        MultipartFile photo,
        String name,
        String source,
        String initialPrice,
        String sellingPrice,
        PurchaseType purchaseType,
        String description,
        String batteryCondition,
        boolean used
    ) {

        try {
            byte[] bytes = toBytes(photo);

            String ocrText = googleCloudVision.detect(bytes);
            PhoneScanDto dto = phoneDataOcrParser.parseRawOcrData(ocrText);

            dto.setName(name);
            dto.setSource(source);
            dto.setInitialPrice(initialPrice);
            dto.setSellingPrice(sellingPrice);
            dto.setDescription(description);
            dto.setUsed(used);
            dto.setBatteryCondition(batteryCondition);
            dto.setPurchaseType(purchaseType);

            return dto;

        } catch (Exception e) {
            log.error("❌ Failed processing photo {}", photo.getOriginalFilename(), e);
            throw e;
        }
    }

    @SneakyThrows
    private byte[] toBytes(MultipartFile file) {
        return file.getInputStream().readAllBytes();
    }


}
