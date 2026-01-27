package pl.gesieniec.gsmseller.phone.scan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
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

    public List<PhoneScanDto> getPhoneScanDtos(
        String name,
        String source,
        String initialPrice,
        String sellingPrice,
        List<MultipartFile> photos,
        PurchaseType purchaseType,
        String description,
        String batteryCondition,
        boolean used) {

        log.info("📦 Starting phone scan process");
        log.info("Metadata: name={}, source={}, initialPrice={}, sellingPrice={}",
            name, source, initialPrice, sellingPrice);

        log.info("Photos count: {}", photos != null ? photos.size() : 0);

        return photos.stream()
            .map(photo -> {
                log.info("➡️ Processing photo: {}", photo.getOriginalFilename());

                byte[] bytes = toBytes(photo);
                log.debug("Photo '{}' byte size: {}", photo.getOriginalFilename(), bytes.length);

                String extractedData;
                try {
                    extractedData = googleCloudVision.detect(bytes);
                } catch (Exception e) {
                    log.error("❌ OCR failed for photo: {}", photo.getOriginalFilename(), e);
                    throw e;
                }

                log.info("➡️ Parsing OCR data for photo: {}", photo.getOriginalFilename());

                PhoneScanDto phoneScanDto;
                try {
                    phoneScanDto = phoneDataOcrParser.parseRawOcrData(extractedData);
                } catch (Exception e) {
                    log.error("❌ OCR parsing failed for photo: {}", photo.getOriginalFilename(), e);
                    throw e;
                }

                phoneScanDto.setName(name);
                phoneScanDto.setSource(source);
                phoneScanDto.setInitialPrice(initialPrice);
                phoneScanDto.setSellingPrice(sellingPrice);
                phoneScanDto.setDescription(description);
                phoneScanDto.setUsed(used);
                phoneScanDto.setBatteryCondition(batteryCondition);
                phoneScanDto.setPurchaseType(purchaseType);

                log.info("✅ PhoneScanDto created for photo: {}", photo.getOriginalFilename());

                return phoneScanDto;
            })
            .filter(this::removeDuplicates)
            .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                    PhoneScanDto::getImei,
                    Function.identity(),
                    (first, second) -> first,
                    LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
            ));

    }

    private boolean removeDuplicates(PhoneScanDto phoneScanDto) {
        if (phoneScanDto.getImei() == null) {
            return true;
        }

        return !phoneStockService.existsActiveDuplicate(phoneScanDto.getImei());
    }

    @SneakyThrows
    private byte[] toBytes(MultipartFile file) {
        log.debug("Reading bytes from MultipartFile: {}", file.getOriginalFilename());
        return file.getBytes();
    }
}
