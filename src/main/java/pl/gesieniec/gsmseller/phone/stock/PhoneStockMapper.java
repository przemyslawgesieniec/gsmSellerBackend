package pl.gesieniec.gsmseller.phone.stock;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneStockMapper {

    public PhoneStock toPhoneStock(PhoneScanDto phoneScanDto){
        return new PhoneStock(phoneScanDto.getModel(),
            phoneScanDto.getRam(),
            phoneScanDto.getMemory(),
            phoneScanDto.getColor(),
            phoneScanDto.getImei1(),
            phoneScanDto.getImei2(),
            phoneScanDto.getEid(),
            phoneScanDto.getBarcode(),
            phoneScanDto.getSn());
    }

    public PhoneStockDto toDto(PhoneStock save) {
        return new PhoneStockDto(save.getModel(),
            save.getRam(),
            save.getMemory(),
            save.getColor(),
            save.getImei1(),
            save.getImei2(),
            save.getEid(),
            save.getBarcode(),
            save.getSn(),
            save.getName(),
            save.getSource(),
            save.getPurchasePrice(),
            save.getSuggestedSellingPrice());
    }
}
