package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferPhotoRepository offerPhotoRepository;
    private final PhoneStockRepository phoneStockRepository;
    private final pl.gesieniec.gsmseller.phone.stock.PhoneStockMapper phoneStockMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public PhoneOffer createOffer(OfferRequest request, List<MultipartFile> photoFiles) {
        log.info("Creating offer for phone technicalId: {}", request.phoneStockTechnicalId());
        PhoneStock phoneStock = phoneStockRepository.findByTechnicalId(request.phoneStockTechnicalId())
            .orElseThrow(() -> {
                log.error("Phone not found for technicalId: {}", request.phoneStockTechnicalId());
                return new EntityNotFoundException("Phone not found: " + request.phoneStockTechnicalId());
            });

        List<OfferPhoto> photos = new ArrayList<>();
        if (photoFiles != null) {
            log.debug("Storing {} photo files for new offer in DB", photoFiles.size());
            photoFiles.forEach(file -> {
                byte[] data = fileStorageService.compressImageIfImage(file);
                photos.add(new OfferPhoto(data, file.getContentType()));
            });
        }

        Offer offer = Offer.builder()
            .phoneStock(phoneStock)
            .screen(request.screen())
            .memory(request.memory())
            .ram(request.ram())
            .simCardType(request.simCardType())
            .frontCamerasMpx(request.frontCamerasMpx())
            .backCamerasMpx(request.backCamerasMpx())
            .batteryCapacity(request.batteryCapacity())
            .communication(request.communication())
            .operatingSystem(request.operatingSystem())
            .brand(request.brand())
            .photos(photos)
            .build();

        PhoneOffer savedOffer = mapToDto(offerRepository.save(offer));
        log.info("Offer created successfully for phone technicalId: {}", request.phoneStockTechnicalId());
        return savedOffer;
    }

    @Transactional
    public PhoneOffer updateOffer(UUID technicalId, OfferRequest request, List<MultipartFile> photoFiles) {
        log.info("Updating offer for technicalId: {}", technicalId);
        Offer offer = offerRepository.findByPhoneStockTechnicalId(technicalId)
            .orElseThrow(() -> {
                log.error("Offer not found for update, technicalId: {}", technicalId);
                return new EntityNotFoundException("Offer not found: " + technicalId);
            });

        List<UUID> requestedExistingPhotoIds = request.photos() != null ? request.photos() : new ArrayList<>();

        // Znajdź istniejące zdjęcia, które mają zostać zachowane
        List<OfferPhoto> finalPhotos = new ArrayList<>(offerPhotoRepository.findAllByTechnicalIdIn(requestedExistingPhotoIds));

        // Dodaj nowe pliki
        if (photoFiles != null) {
            log.debug("Storing {} new photo files for offer update in DB", photoFiles.size());
            photoFiles.forEach(file -> {
                if (!file.isEmpty()) {
                    byte[] data = fileStorageService.compressImageIfImage(file);
                    finalPhotos.add(new OfferPhoto(data, file.getContentType()));
                }
            });
        }

        offer.update(
            request.screen(),
            request.memory(),
            request.ram(),
            request.simCardType(),
            request.frontCamerasMpx(),
            request.backCamerasMpx(),
            request.batteryCapacity(),
            request.communication(),
            request.operatingSystem(),
            request.brand(),
            finalPhotos
        );

        PhoneOffer updatedOffer = mapToDto(offerRepository.save(offer));
        log.info("Offer updated successfully for technicalId: {}", technicalId);
        return updatedOffer;
    }

    @Transactional(readOnly = true)
    public List<OfferPhoto> getPhotos(List<UUID> photoIds) {
        log.debug("Fetching photos for UUIDs: {}", photoIds);
        return offerPhotoRepository.findAllByTechnicalIdIn(photoIds);
    }

    @Transactional(readOnly = true)
    public PhoneOffer getOffer(UUID technicalId) {
        log.debug("Fetching offer for technicalId: {}", technicalId);
        return offerRepository.findByPhoneStockTechnicalId(technicalId)
            .map(this::mapToDto)
            .orElseThrow(() -> {
                log.warn("Offer not found for technicalId: {}", technicalId);
                return new EntityNotFoundException("Offer not found for technicalId: " + technicalId);
            });
    }

    @Transactional(readOnly = true)
    public Page<pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto> getAvailablePhones(String search, Pageable pageable) {
        log.debug("Fetching available phones with search: '{}', pageable: {}", search, pageable);
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
        log.info("Fetching offers with spec and pageable: {}", pageable);
        return offerRepository.findAll(spec, pageable)
            .map(this::mapToDto);
    }

    private PhoneOffer mapToDto(Offer offer) {
        PhoneStock phoneStock = offer.getPhoneStock();
        return PhoneOffer.builder()
            .technicalId(phoneStock.getTechnicalId())
            .price(phoneStock.getSellingPrice())
            .brand(offer.getBrand())
            .model(phoneStock.getModel())
            .status(phoneStock.isUsed() ? "Używany" : "Nowy")
            .color(phoneStock.getColor())
            .location(phoneStock.getLocation() != null ? phoneStock.getLocation().getName() : "Dostępny online")
            .screen(offer.getScreen())
            .memory(offer.getMemory())
            .ram(offer.getRam())
            .simCardType(offer.getSimCardType())
            .frontCamerasMpx(offer.getFrontCamerasMpx())
            .backCamerasMpx(offer.getBackCamerasMpx())
            .batteryCapacity(offer.getBatteryCapacity())
            .communication(offer.getCommunication())
            .operatingSystem(offer.getOperatingSystem())
            .photos(offer.getPhotos().stream().map(OfferPhoto::getTechnicalId).toList())
            .build();
    }
}
