package pl.gesieniec.gsmseller.phone.stock;

import lombok.RequiredArgsConstructor;
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
}
