package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserRepository;

@Service
@RequiredArgsConstructor
public class PhoneStockService implements PhoneSoldHandler {

    private final PhoneStockRepository repository;
    private final PhoneStockMapper phoneStockMapper;
    private final UserRepository userRepository;

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


        Pageable pageable = PageRequest.of(page, size, Sort.by("name").descending());

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
            dto.getSellingPrice()
        );

        return phoneStockMapper.toDto(phone);
    }

    @Transactional
    public void saveAllPhone(List<PhoneScanDto> phoneScanDtoList) {
        phoneScanDtoList.stream()
            .map(phoneStockMapper::toPhoneStock)
            .forEach(repository::save);
    }

    @Override
    @Transactional
    public void markPhoneSold(UUID technicalId, BigDecimal soldPrice) {
        repository.findByTechnicalId(technicalId).ifPresent(e -> e.sell(soldPrice));
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

}
