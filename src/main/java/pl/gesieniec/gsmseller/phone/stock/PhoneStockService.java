package pl.gesieniec.gsmseller.phone.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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
                                         String imei1,
                                         String imei2,
                                         int page,
                                         int size) {
        Specification<PhoneStock> spec = Specification
            .where(PhoneStockSpecifications.hasName(name))
            .and(PhoneStockSpecifications.hasModel(model))
            .and(PhoneStockSpecifications.hasImei1(imei1))
            .and(PhoneStockSpecifications.hasImei2(imei2));

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").descending());

        return repository.findAll(spec, pageable)
            .map(phoneStockMapper::toDto);
    }
}
