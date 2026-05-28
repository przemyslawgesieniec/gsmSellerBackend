package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;
import org.springframework.context.event.EventListener;
import pl.gesieniec.gsmseller.offer.event.OfferCreatedEvent;
import pl.gesieniec.gsmseller.offer.event.OfferRemovedEvent;
import pl.gesieniec.gsmseller.reservation.ReservationCreatedEvent;
import pl.gesieniec.gsmseller.reservation.ReservationExpiredEvent;
import pl.gesieniec.gsmseller.reservation.ReservationCancelledEvent;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.phone.model.PhoneModels;
import pl.gesieniec.gsmseller.phone.model.PhoneModelsService;
import pl.gesieniec.gsmseller.phone.model.PhoneModelsRepository;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;
import pl.gesieniec.gsmseller.phone.stock.event.PhoneRemovedEvent;
import pl.gesieniec.gsmseller.phone.stock.handler.PhoneReturnHandler;
import pl.gesieniec.gsmseller.phone.stock.handler.PhoneSoldHandler;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneHandedOverEvent;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneSoldEvent;
import pl.gesieniec.gsmseller.phone.stock.model.HandoverRequest;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneModelAssignmentRequest;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto;
import pl.gesieniec.gsmseller.phone.stock.model.Status;
import pl.gesieniec.gsmseller.repair.Repair;
import pl.gesieniec.gsmseller.repair.RepairRepository;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneStockService implements PhoneSoldHandler, PhoneReturnHandler {

    private final PhoneStockRepository repository;
    private final PhoneStockMapper phoneStockMapper;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RepairRepository repairRepository;
    private final pl.gesieniec.gsmseller.repair.RepairService repairService;
    private final PhoneModelsRepository phoneModelsRepository;


    public PhoneStockDto getByTechnicalId(UUID id) {
        return repository.findByTechnicalId(id)
            .map(phoneStockMapper::toDto)
            .orElse(null);
    }


    public Page<PhoneStockDto> getPhones(String name,
                                         UUID brand,
                                         UUID model,
                                         String color,
                                         String imei,
                                         Status status,
                                         String locationName,
                                         BigDecimal priceMin,
                                         BigDecimal priceMax,
                                         Boolean hasOffer,
                                         Boolean afterService,
                                         int page,
                                         int size) {
        List<String> brandNames = findBrandNamesByTechnicalId(brand);

        Specification<PhoneStock> spec = Specification
            .where(PhoneStockSpecifications.hasName(name))
            .and(PhoneStockSpecifications.hasPhoneModelBrandIn(brand, brandNames))
            .and(PhoneStockSpecifications.hasPhoneModelTechnicalId(model))
            .and(PhoneStockSpecifications.hasImeiLike(imei))
            .and(PhoneStockSpecifications.hasColor(color))
            .and(PhoneStockSpecifications.hasStatus(status))
            .and(PhoneStockSpecifications.hasLocationName(locationName))
            .and(PhoneStockSpecifications.hasPriceMin(priceMin))
            .and(PhoneStockSpecifications.hasPriceMax(priceMax))
            .and(PhoneStockSpecifications.hasOffer(hasOffer))
            .and(PhoneStockSpecifications.hasAfterService(afterService));


        Pageable pageable = PageRequest.of(page, size, Sort.by("createDateTime").descending());

        return repository.findAll(spec, pageable)
            .map(phoneStockMapper::toDto);
    }

    private List<String> findBrandNamesByTechnicalId(UUID brandTechnicalId) {
        if (brandTechnicalId == null) {
            return List.of();
        }

        return phoneModelsRepository.findAll(Sort.by("brand").ascending())
            .stream()
            .map(PhoneModels::getBrand)
            .filter(phoneBrand -> phoneBrand != null && !phoneBrand.isBlank())
            .filter(phoneBrand -> PhoneModelsService.brandTechnicalId(phoneBrand).equals(brandTechnicalId))
            .map(String::trim)
            .distinct()
            .toList();
    }

    @Transactional
    public PhoneStockDto updatePhone(UUID technicalId, PhoneStockDto dto) {
        PhoneStock phone = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Phone not found: " + technicalId));

        phone.update(
            dto.getModel(),
            dto.getRam(),
            dto.getMemory(),
            dto.getColor(),
            dto.getSimCardType(),
            dto.getImei(),
            dto.getName(),
            dto.getSource(),
            dto.getSellingPrice(),
            dto.getPurchasePrice(),
            dto.getDescription(),
            dto.isUsed(),
            dto.getBatteryCondition(),
            dto.getComment()
        );

        if (dto.getPhoneModelTechnicalId() != null) {
            PhoneModels phoneModel = phoneModelsRepository.findByTechnicalId(dto.getPhoneModelTechnicalId())
                .orElseThrow(() -> new EntityNotFoundException("Phone model not found: " + dto.getPhoneModelTechnicalId()));
            phone.assignPhoneModel(phoneModel);
        }


        return phoneStockMapper.toDto(phone);
    }

    @Transactional
    public void saveAllPhone(List<PhoneScanDto> phoneScanDtoList, String username) {

        LocationEntity locationEntity = userRepository.findByUsername(username)
            .map(User::getLocation)
            .orElse(null);

        phoneScanDtoList.forEach(dto -> {
            PhoneStock entity = phoneStockMapper.toPhoneStock(dto);

            if (dto.getPhoneModelTechnicalId() != null) {
                PhoneModels phoneModel = phoneModelsRepository.findByTechnicalId(dto.getPhoneModelTechnicalId())
                    .orElseThrow(() -> new EntityNotFoundException("Phone model not found: " + dto.getPhoneModelTechnicalId()));
                entity.assignPhoneModel(phoneModel);
            }

            if (locationEntity != null) {
                entity.acceptAtLocation(locationEntity);
            }

            if (dto.isDamaged()) {
                entity.moveToService();
            }

            repository.save(entity);

            if (dto.isDamaged()) {
                Repair repair = Repair.createInHouseRepair(
                    entity.getName(),
                    entity.getImei(),
                    entity.getColor(),
                    entity.getPurchasePrice(),
                    null, // repairPrice not known yet
                    "Telefon dodany jako uszkodzony",
                    null,
                    null,
                    entity.getTechnicalId(),
                    repairService.getNextBusinessId(),
                    locationEntity != null ? locationEntity.getName() : null
                );
                repairRepository.save(repair);
            }
        });
    }

    @Transactional
    public PhoneStockDto assignPhoneModel(UUID technicalId, PhoneModelAssignmentRequest request) {
        if (request.getPhoneModelTechnicalId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model z bazy jest wymagany");
        }

        PhoneStock phone = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Phone not found: " + technicalId));

        PhoneModels phoneModel = phoneModelsRepository.findByTechnicalId(request.getPhoneModelTechnicalId())
            .orElseThrow(() -> new EntityNotFoundException("Phone model not found: " + request.getPhoneModelTechnicalId()));

        phone.assignPhoneModel(phoneModel);
        phone.update(
            null,
            request.getRam(),
            request.getMemory(),
            request.getColor(),
            request.getSimCardType(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        return phoneStockMapper.toDto(phone);
    }

    @Override
    @Transactional
    public void markPhoneSold(UUID technicalId, BigDecimal soldPrice, String sellingInfo) {
        repository.findByTechnicalId(technicalId).ifPresent(e -> {
            e.sell(soldPrice, sellingInfo);
            eventPublisher.publishEvent(new PhoneSoldEvent(technicalId));
        });
    }

    public void acceptPhone(UUID technicalId, String username) {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        LocationEntity userLocation = user.getLocation();
        if (userLocation == null) {
            throw new RuntimeException("User has no assigned location");
        }

        PhoneStock phone = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Phone not found"));

        phone.acceptAtLocation(userLocation);

        repository.save(phone);
    }

    public PhoneStock validateCanBeAddedToCart(UUID technicalId, String username) {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        PhoneStock phone = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Phone not found: " + technicalId));

        if (phone.getStatus() != Status.DOSTĘPNY) {
            throw new IllegalStateException("Telefon nie jest dostępny do sprzedaży");
        }

        if (phone.getLocation() == null) {
            throw new IllegalStateException("Telefon nie jest przypisany do lokalizacji");
        }

        if (user.getLocation() == null) {
            throw new IllegalStateException("Użytkownik nie ma przypisanej lokalizacji");
        }

        if (!phone.getLocation().getId().equals(user.getLocation().getId())) {
            throw new IllegalStateException("Telefon znajduje się w innej lokalizacji");
        }

        return phone;
    }


    @Override
    @Transactional
    public void returnPhones(List<UUID> phoneTechnicalIds) {

        for (UUID phoneId : phoneTechnicalIds) {

            PhoneStock phone = repository
                .findByTechnicalId(phoneId)
                .orElseThrow(() ->
                    new IllegalStateException("Phone not found: " + phoneId)
                );

            phone.returnPhone();
            log.info("Telefon zwrócony do sklepu: {}", phone);
        }
    }

    @Transactional
    public void removePhone(UUID technicalId) {

        PhoneStock phone = repository
            .findByTechnicalId(technicalId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Phone not found"
            ));

        try {
            phone.remove();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, e.getMessage()
            );
        }

        eventPublisher.publishEvent(
            new PhoneRemovedEvent(technicalId)
        );
    }

    @Transactional
    public void restorePhone(UUID technicalId) {
        PhoneStock phone = repository
            .findByTechnicalId(technicalId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Phone not found"
            ));

        try {
            phone.restore();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, e.getMessage()
            );
        }

        repository.save(phone);
    }

    public void removePhoneFromLocation(UUID technicalId) {

        PhoneStock phone = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Phone not found"));

        phone.removeFromLocation();

        repository.save(phone);
    }

    @Transactional
    public void handoverPhone(
        UUID technicalId,
        HandoverRequest request,
        String username
    ) {
        PhoneStock phone = repository.findByTechnicalId(technicalId)
            .orElseThrow(() ->
                new EntityNotFoundException("Phone not found: " + technicalId)
            );

        phone.handover(request.getComment(), request.getPrice());

        eventPublisher.publishEvent(new PhoneHandedOverEvent(technicalId));

        log.info(
            "Telefon {} przekazany przez {}. Komentarz: {}",
            technicalId,
            username,
            request
        );
    }

    public Set<String> findActiveImeis(Collection<String> imeis) {

        if (imeis == null || imeis.isEmpty()) {
            return Set.of();
        }

        return repository.findImeisByStatusIn(
            imeis,
            List.of(Status.DOSTĘPNY, Status.WPROWADZONY, Status.SERWIS)
        );
    }

    @EventListener
    @Transactional
    public void onReservationCreated(ReservationCreatedEvent event) {
        repository.findByTechnicalId(event.technicalId())
            .ifPresent(phone -> phone.setReserved(event.reserved()));
    }

    @EventListener
    @Transactional
    public void onReservationExpired(ReservationExpiredEvent event) {
        repository.findByTechnicalId(event.technicalId())
            .ifPresent(phone -> phone.setReserved(event.reserved()));
    }

    @EventListener
    @Transactional
    public void onReservationCancelled(ReservationCancelledEvent event) {
        repository.findByTechnicalId(event.technicalId())
            .ifPresent(phone -> phone.setReserved(event.reserved()));
    }

    @EventListener
    @Transactional
    public void onOfferCreated(OfferCreatedEvent event) {
        repository.findByTechnicalId(event.phoneStockTechnicalId())
            .ifPresent(phone -> phone.setHasOffer(true));
    }

    @EventListener
    @Transactional
    public void onOfferRemoved(OfferRemovedEvent event) {
        repository.findByTechnicalId(event.phoneStockTechnicalId())
            .ifPresent(phone -> phone.setHasOffer(false));
    }

}
