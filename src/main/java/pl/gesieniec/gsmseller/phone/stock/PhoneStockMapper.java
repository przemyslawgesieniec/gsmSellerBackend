package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto;

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
            new BigDecimal(phoneScanDto.getInitialPrice()),
            new BigDecimal(phoneScanDto.getSellingPrice()),
            phoneScanDto.getPurchaseType());
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
            save.getSellingPrice(),
            Optional.ofNullable(save.getLocation())
                .map(LocationEntity::getName).orElse(null),
            save.getPurchaseType(),
            save.getComment());
    }
}
