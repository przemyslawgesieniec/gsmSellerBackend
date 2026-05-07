package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto;

@Component
@RequiredArgsConstructor
public class PhoneStockMapper {

    private static DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    public PhoneStock toPhoneStock(PhoneScanDto phoneScanDto) {
        return PhoneStock.create(
            phoneScanDto.getModel(),
            phoneScanDto.getRam(),
            phoneScanDto.getMemory(),
            phoneScanDto.getColor(),
            phoneScanDto.getSimCardType(),
            phoneScanDto.getImei(),
            phoneScanDto.getName(),
            phoneScanDto.getSource(),
            new BigDecimal(phoneScanDto.getInitialPrice()),
            new BigDecimal(phoneScanDto.getSellingPrice()),
            phoneScanDto.getPurchaseType(),
            phoneScanDto.getComment(),
            phoneScanDto.getDescription(),
            phoneScanDto.getBatteryCondition(),
            phoneScanDto.isUsed());
    }

    public PhoneStockDto toDto(PhoneStock save) {
        return new PhoneStockDto(
            save.getTechnicalId(),
            save.getModel(),
            save.getRam(),
            save.getMemory(),
            save.getColor(),
            save.getSimCardType(),
            save.getImei(),
            save.getName(),
            save.getSource(),
            save.getStatus(),
            save.getPurchasePrice(),
            save.getSellingPrice(),
            save.getSoldFor(),
            save.getCreateDateTime().format(formatter),
            Optional.ofNullable(save.getLocation())
                .map(LocationEntity::getName).orElse(null),
            save.getPurchaseType(),
            save.getComment(),
            save.getDescription(),
            save.getBatteryCondition(),
            save.isUsed(),
            save.isReserved(),
            save.isHasOffer(),
            save.isAfterService(),
            Optional.ofNullable(save.getPhoneModel())
                .map(pl.gesieniec.gsmseller.phone.model.PhoneModels::getTechnicalId)
                .orElse(null),
            Optional.ofNullable(save.getPhoneModel())
                .map(pl.gesieniec.gsmseller.phone.model.PhoneModels::getDisplayName)
                .orElse(null));
    }
}
