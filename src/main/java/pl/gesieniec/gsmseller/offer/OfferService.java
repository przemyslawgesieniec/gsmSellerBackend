package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;
import pl.gesieniec.gsmseller.offer.model.OfferRequest;
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;
import pl.gesieniec.gsmseller.offer.model.Photo;
import pl.gesieniec.gsmseller.offer.model.PublicPhoneOffer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.gesieniec.gsmseller.offer.event.OfferCreatedEvent;
import pl.gesieniec.gsmseller.offer.event.OfferRemovedEvent;
import pl.gesieniec.gsmseller.offer.model.specs.CommunicationSpecs;
import pl.gesieniec.gsmseller.offer.model.specs.ScreenSpecs;
import pl.gesieniec.gsmseller.phone.model.PhoneModels;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneHandedOverEvent;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneSoldEvent;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.storage.FileStorageService;
import pl.gesieniec.gsmseller.reservation.ReservationCreatedEvent;
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
    private final CloudflareImagesService cloudflareImagesService;
    private final ApplicationEventPublisher eventPublisher;

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
            log.debug("Storing {} photo files for new offer", photoFiles.size());
            photoFiles.forEach(file -> {
                try {
                    String imageId = cloudflareImagesService.uploadImage(file);
                    photos.add(new OfferPhoto(file.getContentType(), imageId));
                } catch (java.io.IOException e) {
                    log.error("Failed to upload image to Cloudflare", e);
                    throw new RuntimeException("Failed to upload image", e);
                }
            });
        }

        PhoneModels phoneModel = getAssignedPhoneModel(phoneStock);
        Offer offer = Offer.builder()
            .phoneStock(phoneStock)
            .photos(photos)
            .isReserved(phoneStock.isReserved())
            .build();
        applyPhoneModelToOffer(offer, phoneStock, phoneModel, request.batteryCapacity());

        PhoneOffer savedOffer = mapToDto(offerRepository.save(offer));
        eventPublisher.publishEvent(new OfferCreatedEvent(request.phoneStockTechnicalId()));
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

        // Znajdź istniejące zdjęcia tej oferty, które mają zostać zachowane
        List<OfferPhoto> existingPhotos = offer.getPhotos();
        java.util.Map<UUID, OfferPhoto> existingPhotosById = existingPhotos.stream()
            .collect(java.util.stream.Collectors.toMap(
                OfferPhoto::getTechnicalId,
                java.util.function.Function.identity()
            ));
        List<OfferPhoto> photosToKeep = requestedExistingPhotoIds.stream()
            .map(existingPhotosById::get)
            .filter(java.util.Objects::nonNull)
            .toList();
        
        // Zdjęcia do usunięcia z Cloudflare
        List<String> imageIdsToDelete = existingPhotos.stream()
            .filter(p -> !photosToKeep.contains(p))
            .map(OfferPhoto::getImageId)
            .filter(java.util.Objects::nonNull)
            .toList();

        List<OfferPhoto> uploadedPhotos = new ArrayList<>();

        // Dodaj nowe pliki
        if (photoFiles != null) {
            log.debug("Storing {} new photo files for offer update", photoFiles.size());
            photoFiles.forEach(file -> {
                if (!file.isEmpty()) {
                    try {
                        String imageId = cloudflareImagesService.uploadImage(file);
                        uploadedPhotos.add(new OfferPhoto(file.getContentType(), imageId));
                    } catch (java.io.IOException e) {
                        log.error("Failed to upload image to Cloudflare", e);
                        throw new RuntimeException("Failed to upload image", e);
                    }
                }
            });
        }

        List<OfferPhoto> finalPhotos = orderPhotos(request.photoOrder(), photosToKeep, uploadedPhotos);

        PhoneModels phoneModel = getAssignedPhoneModel(offer.getPhoneStock());
        offer.updateSpecifications(
            ScreenSpecs.builder()
                .size(phoneModel.getScreen())
                .resolution(phoneModel.getScreenResolution())
                .type(phoneModel.getDisplayType())
                .build(),
            valueOrFallback(offer.getPhoneStock().getMemory(), phoneModel.getMemory()),
            valueOrFallback(offer.getPhoneStock().getRam(), phoneModel.getRam()),
            valueOrFallback(offer.getPhoneStock().getSimCardType(), phoneModel.getSimCardType()),
            phoneModel.getFrontCamerasMpx(),
            phoneModel.getBackCamerasMpx(),
            resolveOfferBatteryCapacity(request.batteryCapacity(), offer.getBatteryCapacity(), phoneModel.getBatteryCapacity()),
            CommunicationSpecs.builder()
                .portType(phoneModel.getPortType())
                .build(),
            null,
            phoneModel.getBrand()
        );
        offer.setPhotos(finalPhotos);

        PhoneOffer updatedOffer = mapToDto(offerRepository.save(offer));

        // Usuń zdjęcia z Cloudflare po udanym zapisie oferty
        imageIdsToDelete.forEach(imageId -> {
            try {
                cloudflareImagesService.deleteImage(imageId);
            } catch (Exception e) {
                log.error("Failed to delete orphaned image {} from Cloudflare", imageId, e);
            }
        });

        log.info("Offer updated successfully for technicalId: {}", technicalId);
        return updatedOffer;
    }

    @Transactional
    public void refreshOffersForPhoneModel(PhoneModels phoneModel) {
        List<Offer> offers = offerRepository.findAllByPhoneStockPhoneModelTechnicalId(phoneModel.getTechnicalId());

        offers.forEach(offer -> applyPhoneModelToOffer(offer, offer.getPhoneStock(), phoneModel, null));

        log.info("Refreshed {} offers for phone model {}", offers.size(), phoneModel.getTechnicalId());
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
    public PublicPhoneOffer getPublicOffer(UUID technicalId) {
        return toPublicOffer(getOffer(technicalId));
    }

    @Transactional(readOnly = true)
    public Page<pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto> getAvailablePhones(String search, Pageable pageable) {
        log.debug("Fetching available phones with search: '{}', pageable: {}", search, pageable);
        Specification<PhoneStock> spec = (root, query, cb) -> {
            query.distinct(true);
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<Offer> offerRoot = subquery.from(Offer.class);
            subquery.select(offerRoot.get("phoneStock").get("id"));

            jakarta.persistence.criteria.Predicate noOffer = cb.not(root.get("id").in(subquery));
            jakarta.persistence.criteria.Predicate isAvailable = cb.equal(root.get("status"), pl.gesieniec.gsmseller.phone.stock.model.Status.DOSTĘPNY);
            jakarta.persistence.criteria.Predicate hasModel = cb.isNotNull(root.get("phoneModel"));

            jakarta.persistence.criteria.Predicate basePredicate = cb.and(noOffer, isAvailable, hasModel);

            if (search == null || search.isBlank()) {
                return basePredicate;
            }

            String pattern = "%" + search.toLowerCase() + "%";
            jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("model")), pattern),
                cb.like(cb.lower(root.get("imei")), pattern),
                cb.like(cb.lower(root.join("phoneModel", jakarta.persistence.criteria.JoinType.LEFT).get("brand")), pattern),
                cb.like(cb.lower(root.join("phoneModel", jakarta.persistence.criteria.JoinType.LEFT).get("model")), pattern)
            );

            return cb.and(basePredicate, searchPredicate);
        };

        return phoneStockRepository.findAll(spec, pageable)
            .map(phoneStockMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PhoneOffer> getOffers(Specification<Offer> spec, Pageable pageable) {
        log.info("Fetching offers with spec and pageable: {}", pageable);
        // Filtrowanie ofert, które mają telefon przypisany
        Specification<Offer> nonNullPhoneSpec = (root, query, cb) -> {
            return cb.isNotNull(root.get("phoneStock"));
        };
        Specification<Offer> finalSpec = Specification.allOf(spec, nonNullPhoneSpec);

        return offerRepository.findAll(finalSpec, pageable)
            .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PublicPhoneOffer> getPublicOffers(Specification<Offer> spec, Pageable pageable) {
        return getOffers(spec, pageable).map(this::toPublicOffer);
    }

    private PhoneOffer mapToDto(Offer offer) {
        PhoneStock phoneStock = offer.getPhoneStock();
        String phoneModelName = phoneStock.getPhoneModel() != null
            ? phoneStock.getPhoneModel().getModel()
            : phoneStock.getModel();
        List<Photo> photos = offer.getPhotos().stream()
            .map(photo -> Photo.builder()
                .uuid(photo.getTechnicalId())
                .thumbnailUrl(cloudflareImagesService.getImageUrl(photo.getImageId(), "thumbnail"))
                .galleryUrl(cloudflareImagesService.getImageUrl(photo.getImageId(), "galery"))
                .publicUrl(cloudflareImagesService.getImageUrl(photo.getImageId(), "public"))
                .build())
            .toList();

        return PhoneOffer.builder()
            .technicalId(phoneStock.getTechnicalId())
            .price(phoneStock.getSellingPrice())
            .brand(offer.getBrand())
            .phoneModelName(phoneModelName)
            .model(phoneStock.getModel())
            .imei(phoneStock.getImei())
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
            .batteryCondition(phoneStock.getBatteryCondition())
            .communication(offer.getCommunication())
            .operatingSystem(offer.getOperatingSystem())
            .photos(photos)
            .isReserved(offer.isReserved())
            .build();
    }

    private PhoneModels getAssignedPhoneModel(PhoneStock phoneStock) {
        if (phoneStock.getPhoneModel() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefon nie ma przypisanego modelu z bazy");
        }
        return phoneStock.getPhoneModel();
    }

    private void applyPhoneModelToOffer(Offer offer, PhoneStock phoneStock, PhoneModels phoneModel, String requestedBatteryCapacity) {
        offer.updateSpecifications(
            ScreenSpecs.builder()
                .size(phoneModel.getScreen())
                .resolution(phoneModel.getScreenResolution())
                .type(phoneModel.getDisplayType())
                .build(),
            valueOrFallback(phoneStock.getMemory(), phoneModel.getMemory()),
            valueOrFallback(phoneStock.getRam(), phoneModel.getRam()),
            valueOrFallback(phoneStock.getSimCardType(), phoneModel.getSimCardType()),
            phoneModel.getFrontCamerasMpx(),
            phoneModel.getBackCamerasMpx(),
            resolveOfferBatteryCapacity(requestedBatteryCapacity, offer.getBatteryCapacity(), phoneModel.getBatteryCapacity()),
            CommunicationSpecs.builder()
                .portType(phoneModel.getPortType())
                .build(),
            null,
            phoneModel.getBrand()
        );
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveOfferBatteryCapacity(String requestedValue, String currentOfferValue, String modelVariants) {
        String requested = trimToNull(requestedValue);
        if (requested != null) {
            return requested;
        }

        String current = trimToNull(currentOfferValue);
        if (current != null) {
            return current;
        }

        return firstModelVariant(modelVariants);
    }

    private String firstModelVariant(String variants) {
        String normalized = trimToNull(variants);
        if (normalized == null) {
            return null;
        }

        return java.util.Arrays.stream(normalized.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .findFirst()
            .orElse(normalized);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PublicPhoneOffer toPublicOffer(PhoneOffer offer) {
        return PublicPhoneOffer.builder()
            .technicalId(offer.technicalId())
            .price(offer.price())
            .brand(offer.brand())
            .phoneModelName(offer.phoneModelName())
            .model(offer.model())
            .status(offer.status())
            .color(offer.color())
            .location(offer.location())
            .screen(offer.screen())
            .memory(offer.memory())
            .ram(offer.ram())
            .simCardType(offer.simCardType())
            .frontCamerasMpx(offer.frontCamerasMpx())
            .backCamerasMpx(offer.backCamerasMpx())
            .batteryCapacity(offer.batteryCapacity())
            .batteryCondition(offer.batteryCondition())
            .communication(offer.communication())
            .operatingSystem(offer.operatingSystem())
            .photos(offer.photos())
            .isReserved(offer.isReserved())
            .build();
    }

    private List<OfferPhoto> orderPhotos(List<String> photoOrder, List<OfferPhoto> existingPhotos, List<OfferPhoto> uploadedPhotos) {
        if (photoOrder == null || photoOrder.isEmpty()) {
            List<OfferPhoto> fallbackOrder = new ArrayList<>(existingPhotos);
            fallbackOrder.addAll(uploadedPhotos);
            return fallbackOrder;
        }

        java.util.Map<String, OfferPhoto> existingById = existingPhotos.stream()
            .collect(java.util.stream.Collectors.toMap(
                photo -> photo.getTechnicalId().toString(),
                java.util.function.Function.identity()
            ));

        List<OfferPhoto> orderedPhotos = new ArrayList<>();
        for (String token : photoOrder) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (token.startsWith("new:")) {
                try {
                    int index = Integer.parseInt(token.substring(4));
                    if (index >= 0 && index < uploadedPhotos.size()) {
                        OfferPhoto photo = uploadedPhotos.get(index);
                        if (!orderedPhotos.contains(photo)) {
                            orderedPhotos.add(photo);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    log.warn("Ignoring invalid photo order token: {}", token);
                }
                continue;
            }

            OfferPhoto photo = existingById.get(token);
            if (photo != null && !orderedPhotos.contains(photo)) {
                orderedPhotos.add(photo);
            }
        }

        existingPhotos.stream()
            .filter(photo -> !orderedPhotos.contains(photo))
            .forEach(orderedPhotos::add);
        uploadedPhotos.stream()
            .filter(photo -> !orderedPhotos.contains(photo))
            .forEach(orderedPhotos::add);

        return orderedPhotos;
    }

    @EventListener
    @Transactional
    public void onPhoneSold(PhoneSoldEvent event) {
        removeOfferByPhoneTechnicalId(event.technicalId(), "sold");
    }

    @EventListener
    @Transactional
    public void onPhoneHandedOver(PhoneHandedOverEvent event) {
        removeOfferByPhoneTechnicalId(event.technicalId(), "handed over");
    }

    @Transactional
    public void deleteOffer(UUID technicalId) {
        log.info("Deleting offer for phone technicalId: {}", technicalId);
        offerRepository.findByPhoneStockTechnicalId(technicalId)
            .ifPresentOrElse(offer -> {
                List<String> imageIds = offer.getPhotos().stream()
                    .map(OfferPhoto::getImageId)
                    .filter(java.util.Objects::nonNull)
                    .toList();

                offerRepository.delete(offer);

                imageIds.forEach(imageId -> {
                    try {
                        cloudflareImagesService.deleteImage(imageId);
                    } catch (Exception e) {
                        log.error("Failed to delete image {} from Cloudflare", imageId, e);
                    }
                });

                eventPublisher.publishEvent(new OfferRemovedEvent(technicalId));
                log.info("Offer for phone technicalId {} deleted successfully", technicalId);
            }, () -> {
                log.warn("Offer for phone technicalId {} not found for deletion", technicalId);
                throw new EntityNotFoundException("Offer not found: " + technicalId);
            });
    }

    private void removeOfferByPhoneTechnicalId(UUID technicalId, String reason) {
        log.info("Removing offer for {} phone: {}", reason, technicalId);
        offerRepository.findByPhoneStockTechnicalId(technicalId)
            .ifPresent(offer -> {
                List<String> imageIds = offer.getPhotos().stream()
                    .map(OfferPhoto::getImageId)
                    .filter(java.util.Objects::nonNull)
                    .toList();

                offerRepository.delete(offer);

                imageIds.forEach(imageId -> {
                    try {
                        cloudflareImagesService.deleteImage(imageId);
                    } catch (Exception e) {
                        log.error("Failed to delete image {} from Cloudflare", imageId, e);
                    }
                });

                eventPublisher.publishEvent(new OfferRemovedEvent(technicalId));
                log.info("Offer for phone {} removed successfully", technicalId);
            });
    }

    @EventListener
    @Transactional
    public void onReservationCreated(ReservationCreatedEvent event) {
        offerRepository.findByPhoneStockTechnicalId(event.technicalId())
            .ifPresent(offer -> offer.setReserved(event.reserved()));
    }
}
