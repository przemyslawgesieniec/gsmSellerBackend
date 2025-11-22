package pl.gesieniec.gsmseller.phone.stock;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneStockMapper {

    public PhoneStock toPhoneStock(PhoneScanDto phoneScanDto){
        //TODO fix me
        return PhoneStock.create(
            phoneScanDto.getModel(),
            phoneScanDto.getRam(),
            phoneScanDto.getMemory(),
            phoneScanDto.getColor(),
            phoneScanDto.getImei1()
            ,null,null,null,null);
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
