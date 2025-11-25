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
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

@Service
@RequiredArgsConstructor
public class PhoneStockService {

    private final PhoneStockRepository repository;
    private final PhoneStockMapper phoneStockMapper;

    public PhoneStockDto savePhone(PhoneScanDto dto) {
        PhoneStock phoneStock = phoneStockMapper.toPhoneStock(dto);
        PhoneStock save = repository.save(phoneStock);
        return phoneStockMapper.toDto(save);
    }

    public Page<PhoneStockDto> getPhones(String name,
                                         String model,
                                         String color,
                                         String imei,
                                         Status status,
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
}
