package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;
import pl.gesieniec.gsmseller.offer.model.OfferRequest;
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.storage.FileStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final PhoneStockRepository phoneStockRepository;
    private final pl.gesieniec.gsmseller.phone.stock.PhoneStockMapper phoneStockMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public PhoneOffer createOffer(OfferRequest request, List<MultipartFile> photoFiles) {
        PhoneStock phoneStock = phoneStockRepository.findByTechnicalId(request.phoneStockTechnicalId())
            .orElseThrow(() -> new EntityNotFoundException("Phone not found: " + request.phoneStockTechnicalId()));

        List<String> photoNames = new ArrayList<>();
        if (photoFiles != null) {
            photoFiles.forEach(file -> photoNames.add(fileStorageService.storeFile(file)));
        }

        Offer offer = new Offer(
            phoneStock,
            request.screenSize(),
            request.batteryCapacity(),
            request.screenType(),
            photoNames
        );

        return mapToDto(offerRepository.save(offer));
    }

    @Transactional
    public PhoneOffer updateOffer(UUID technicalId, OfferRequest request, List<MultipartFile> photoFiles) {
        Offer offer = offerRepository.findByPhoneStockTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Offer not found: " + technicalId));

        List<String> currentPhotos = offer.getPhotos();
        List<String> requestedExistingPhotos = request.photos() != null ? request.photos() : new ArrayList<>();

        // Usuń pliki, których nie ma już w request
        currentPhotos.stream()
            .filter(photo -> !requestedExistingPhotos.contains(photo))
            .forEach(fileStorageService::deleteFile);

        List<String> finalPhotos = new ArrayList<>(requestedExistingPhotos);

        // Dodaj nowe pliki
        if (photoFiles != null) {
            photoFiles.forEach(file -> finalPhotos.add(fileStorageService.storeFile(file)));
        }

        offer.update(
            request.screenSize(),
            request.batteryCapacity(),
            request.screenType(),
            finalPhotos
        );

        return mapToDto(offerRepository.save(offer));
    }

    @Transactional(readOnly = true)
    public PhoneOffer getOffer(UUID technicalId) {
        return offerRepository.findByPhoneStockTechnicalId(technicalId)
            .map(this::mapToDto)
            .orElseThrow(() -> new EntityNotFoundException("Offer not found for technicalId: " + technicalId));
    }

    @Transactional(readOnly = true)
    public Page<pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto> getAvailablePhones(String search, Pageable pageable) {
        Specification<PhoneStock> spec = (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<Offer> offerRoot = subquery.from(Offer.class);
            subquery.select(offerRoot.get("phoneStock").get("id"));

            jakarta.persistence.criteria.Predicate noOffer = cb.not(root.get("id").in(subquery));

            if (search == null || search.isBlank()) {
                return noOffer;
            }

            String pattern = "%" + search.toLowerCase() + "%";
            jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("model")), pattern),
                cb.like(cb.lower(root.get("imei")), pattern)
            );

            return cb.and(noOffer, searchPredicate);
        };

        return phoneStockRepository.findAll(spec, pageable)
            .map(phoneStockMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PhoneOffer> getOffers(Specification<Offer> spec, Pageable pageable) {
        return offerRepository.findAll(spec, pageable)
            .map(this::mapToDto);
    }

    private PhoneOffer mapToDto(Offer offer) {
        return PhoneOffer.builder()
            .technicalId(offer.getPhoneStock().getTechnicalId())
            .price(offer.getPhoneStock().getSellingPrice())
            .brand(offer.getPhoneStock().getName()) // Przyjmuję, że 'name' zawiera markę lub jest jej odpowiednikiem, PhoneStock nie ma pola 'brand'
            .model(offer.getPhoneStock().getModel())
            .screenSize(offer.getScreenSize())
            .batteryCapacity(offer.getBatteryCapacity())
            .screenType(offer.getScreenType())
            .photos(offer.getPhotos())
            .build();
    }
}
