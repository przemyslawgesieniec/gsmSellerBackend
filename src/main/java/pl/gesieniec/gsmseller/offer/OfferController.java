package pl.gesieniec.gsmseller.offer;

import kotlin.SinceKotlin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.offer.model.OfferRequest;
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;
import pl.gesieniec.gsmseller.offer.model.specs.CommunicationSpecs;
import pl.gesieniec.gsmseller.offer.model.specs.ScreenSpecs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferController {

    private final OfferService offerService;
    private final OfferOpenAIParser offerOpenAIParser;

    @GetMapping("/ai-specs")
    public PhoneOffer getAiSpecs(@RequestParam String name, @RequestParam String model) {
        return offerOpenAIParser.fetchSpecsFromAi(name, model);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhoneOffer createOffer(
        @RequestParam UUID phoneStockTechnicalId,
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String screenSize,
        @RequestParam(required = false) String screenResolution,
        @RequestParam(required = false) String screenType,
        @RequestParam(required = false) String memory,
        @RequestParam(required = false) String ram,
        @RequestParam(required = false) String simCardType,
        @RequestParam(required = false) List<Integer> frontCamerasMpx,
        @RequestParam(required = false) List<Integer> backCamerasMpx,
        @RequestParam(required = false) String batteryCapacity,
        @RequestParam(required = false) String wifi,
        @RequestParam(required = false) String portType,
        @RequestParam(required = false) String bluetoothVersion,
        @RequestParam(required = false) String operatingSystem,
        @RequestParam(required = false) List<MultipartFile> photoFiles
    ) {
        OfferRequest request = OfferRequest.builder()
            .phoneStockTechnicalId(phoneStockTechnicalId)
            .brand(brand)
            .screen(ScreenSpecs.builder()
                .size(screenSize)
                .resolution(screenResolution)
                .type(screenType)
                .build())
            .memory(memory)
            .ram(ram)
            .simCardType(simCardType)
            .frontCamerasMpx(frontCamerasMpx)
            .backCamerasMpx(backCamerasMpx)
            .batteryCapacity(batteryCapacity)
            .communication(CommunicationSpecs.builder()
                .wifi(wifi)
                .portType(portType)
                .bluetoothVersion(bluetoothVersion)
                .build())
            .operatingSystem(operatingSystem)
            .build();
        return offerService.createOffer(request, photoFiles);
    }

    @PutMapping(value = "/{technicalId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhoneOffer updateOffer(
        @PathVariable UUID technicalId,
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String screenSize,
        @RequestParam(required = false) String screenResolution,
        @RequestParam(required = false) String screenType,
        @RequestParam(required = false) String memory,
        @RequestParam(required = false) String ram,
        @RequestParam(required = false) String simCardType,
        @RequestParam(required = false) List<Integer> frontCamerasMpx,
        @RequestParam(required = false) List<Integer> backCamerasMpx,
        @RequestParam(required = false) String batteryCapacity,
        @RequestParam(required = false) String wifi,
        @RequestParam(required = false) String portType,
        @RequestParam(required = false) String bluetoothVersion,
        @RequestParam(required = false) String operatingSystem,
        @RequestParam(required = false) List<MultipartFile> photoFiles,
        @RequestParam(required = false) List<UUID> photos
    ) {
        OfferRequest request = OfferRequest.builder()
            .brand(brand)
            .screen(ScreenSpecs.builder()
                .size(screenSize)
                .resolution(screenResolution)
                .type(screenType)
                .build())
            .memory(memory)
            .ram(ram)
            .simCardType(simCardType)
            .frontCamerasMpx(frontCamerasMpx)
            .backCamerasMpx(backCamerasMpx)
            .batteryCapacity(batteryCapacity)
            .communication(CommunicationSpecs.builder()
                .wifi(wifi)
                .portType(portType)
                .bluetoothVersion(bluetoothVersion)
                .build())
            .operatingSystem(operatingSystem)
            .photos(photos)
            .build();
        return offerService.updateOffer(technicalId, request, photoFiles);
    }

    @DeleteMapping("/{technicalId}")
    public void deleteOffer(@PathVariable UUID technicalId) {
        offerService.deleteOffer(technicalId);
    }

    @GetMapping("/available-phones")
    public Page<pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto> getAvailablePhones(
        @RequestParam(required = false) String search,
        @PageableDefault(sort = "createDateTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return offerService.getAvailablePhones(search, pageable);
    }
}
