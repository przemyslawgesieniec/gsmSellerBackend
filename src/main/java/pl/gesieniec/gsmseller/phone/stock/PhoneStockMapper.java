package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneStockMapper {

    public PhoneStock toPhoneStock(PhoneScanDto phoneScanDto){
        return PhoneStock.create(
            phoneScanDto.getModel(),
            phoneScanDto.getRam(),
            phoneScanDto.getMemory(),
            phoneScanDto.getColor(),
            phoneScanDto.getImei(),
            phoneScanDto.getName(),
            phoneScanDto.getSource(),
            new BigDecimal(phoneScanDto.getSellingPrice()),
            new BigDecimal(phoneScanDto.getInitialPrice()));
    }

    public PhoneStockDto toDto(PhoneStock save) {
        return new PhoneStockDto(
            save.getTechnicalId(),
            save.getModel(),
            save.getRam(),
            save.getMemory(),
            save.getColor(),
            save.getImei(),
            save.getName(),
            save.getSource(),
            save.getStatus(),
            save.getPurchasePrice(),
            save.getSellingPrice());
    }
}
