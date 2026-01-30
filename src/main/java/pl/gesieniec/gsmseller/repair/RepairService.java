package pl.gesieniec.gsmseller.repair;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.location.LocationRepository;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.repair.model.RepairDto;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;
import pl.gesieniec.gsmseller.repair.model.RestoreToShopRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairRepository repository;
    private final RepairMapper mapper;
    private final RepairPdfService pdfService;
    private final PhoneStockRepository phoneStockRepository;
    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<RepairDto> getAllRepairs() {
        return repository.findAll().stream()
            .filter(r -> r.getStatus() != RepairStatus.ARCHIWALNA)
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(UUID technicalId) {
        Repair repair = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));
        if (!repair.isForCustomer()) {
            throw new IllegalStateException("Receipts are only available for customer repairs");
        }
        return pdfService.generateRepairReceiptPdf(repair);
    }

    @Transactional(readOnly = true)
    public byte[] generateHandoverPdf(UUID technicalId) {
        Repair repair = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));
        if (!repair.isForCustomer()) {
            throw new IllegalStateException("Handover documents are only available for customer repairs");
        }
        return pdfService.generateRepairHandoverPdf(repair);
    }

    @Transactional
    public RepairDto addRepair(RepairDto dto) {
        Repair repair = Repair.create(
            dto.getName(),
            dto.getImei(),
            dto.getColor(),
            dto.getPurchasePrice(),
            dto.getRepairPrice(),
            dto.getDamageDescription(),
            dto.getRepairOrderDescription(),
            dto.getPinPassword(),
            dto.isForCustomer(),
            dto.getPhoneTechnicalId()
        );
        Repair saved = repository.save(repair);
        return mapper.toDto(saved);
    }

    @Transactional
    public RepairDto updateRepair(UUID technicalId, RepairDto dto) {
        Repair repair = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));

        repair.update(
            dto.getName(),
            dto.getImei(),
            dto.getColor(),
            dto.getPurchasePrice(),
            dto.getRepairPrice(),
            dto.getDamageDescription(),
            dto.getRepairOrderDescription(),
            dto.getPinPassword(),
            dto.isForCustomer(),
            dto.getPhoneTechnicalId()
        );

        return mapper.toDto(repair);
    }

    @Transactional
    public RepairDto updateStatus(UUID technicalId, RepairStatus status) {
        Repair repair = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));

        repair.updateStatus(status);
        return mapper.toDto(repair);
    }

    @Transactional(readOnly = true)
    public RepairDto getRepair(UUID technicalId) {
        return repository.findByTechnicalId(technicalId)
            .map(mapper::toDto)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));
    }

    @Transactional
    public void restoreToShop(UUID technicalId, RestoreToShopRequest request) {
        Repair repair = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));

        if (repair.isForCustomer() || repair.getPhoneTechnicalId() == null) {
            throw new IllegalStateException("This repair is not linked to a shop phone");
        }

        PhoneStock phone = phoneStockRepository.findByTechnicalId(repair.getPhoneTechnicalId())
            .orElseThrow(() -> new RuntimeException("Associated phone not found: " + repair.getPhoneTechnicalId()));

        LocationEntity location = locationRepository.findByTechnicalId(request.getLocationTechnicalId())
            .orElseThrow(() -> new RuntimeException("Location not found: " + request.getLocationTechnicalId()));

        // Update phone
        phone.acceptAtLocation(location);
        phone.update(
            null, // model
            null, // ram
            null, // memory
            null, // color
            null, // imei
            null, // name
            null, // source
            request.getSellingPrice(),
            null, // purchasePrice
            null, // description
            null, // isUsed
            null, // batteryCondition
            null  // comment
        );

        // Update repair
        repair.update(
            null, null, null, null,
            request.getRepairPrice(),
            null, null, null,
            null, null
        );
        repair.updateStatus(RepairStatus.ARCHIWALNA);

        phoneStockRepository.save(phone);
        repository.save(repair);
    }
}
