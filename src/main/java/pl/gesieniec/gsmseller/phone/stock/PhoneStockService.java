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
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;
import pl.gesieniec.gsmseller.phone.stock.event.PhoneRemovedEvent;
import pl.gesieniec.gsmseller.phone.stock.handler.PhoneReturnHandler;
import pl.gesieniec.gsmseller.phone.stock.handler.PhoneSoldHandler;
import pl.gesieniec.gsmseller.phone.stock.model.HandoverRequest;
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


    public PhoneStockDto getByTechnicalId(UUID id) {
        return repository.findByTechnicalId(id)
            .map(phoneStockMapper::toDto)
            .orElse(null);
    }


    public Page<PhoneStockDto> getPhones(String name,
                                         String model,
                                         String color,
                                         String imei,
                                         Status status,
                                         String LocationName,
                                         BigDecimal priceMin,
                                         BigDecimal priceMax,
                                         int page,
                                         int size) {
        Specification<PhoneStock> spec = Specification
            .where(PhoneStockSpecifications.hasName(name))
            .and(PhoneStockSpecifications.hasModel(model))
            .and(PhoneStockSpecifications.hasImeiLike(imei))
            .and(PhoneStockSpecifications.hasColor(color))
            .and(PhoneStockSpecifications.hasStatus(status))
            .and(PhoneStockSpecifications.hasLocationName(LocationName))
            .and(PhoneStockSpecifications.hasPriceMin(priceMin))
            .and(PhoneStockSpecifications.hasPriceMax(priceMax));


        Pageable pageable = PageRequest.of(page, size, Sort.by("createDateTime").descending());

        return repository.findAll(spec, pageable)
            .map(phoneStockMapper::toDto);
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
            dto.getImei(),
            dto.getName(),
            dto.getSource(),
            dto.getSellingPrice(),
            dto.getPurchasePrice(),
            dto.getDescription(),
            dto.getUsed(),
            dto.getBatteryCondition(),
            dto.getComment()
        );


        return phoneStockMapper.toDto(phone);
    }

    @Transactional
    public void saveAllPhone(List<PhoneScanDto> phoneScanDtoList, String username) {

        LocationEntity locationEntity = userRepository.findByUsername(username)
            .map(User::getLocation)
            .orElse(null);

        phoneScanDtoList.forEach(dto -> {
            PhoneStock entity = phoneStockMapper.toPhoneStock(dto);
            if (locationEntity != null) {
                entity.acceptAtLocation(locationEntity);
            }

            if (dto.isDamaged()) {
                entity.moveToService();
            }

            repository.save(entity);

            if (dto.isDamaged()) {
                Repair repair = Repair.createInHouseRepair(
                    entity.getModel(),
                    entity.getImei(),
                    entity.getPurchasePrice(),
                    null, // repairPrice not known yet
                    "Telefon dodany jako uszkodzony",
                    null,
                    null,
                    entity.getTechnicalId()
                );
                repairRepository.save(repair);
            }
        });
    }

    @Override
    @Transactional
    public void markPhoneSold(UUID technicalId, BigDecimal soldPrice, String sellingInfo) {
        repository.findByTechnicalId(technicalId).ifPresent(e -> e.sell(soldPrice, sellingInfo));
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


}
