package pl.gesieniec.gsmseller.repair;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.location.LocationRepository;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.repair.client.RepairClient;
import pl.gesieniec.gsmseller.repair.client.RepairClientRepository;
import pl.gesieniec.gsmseller.repair.model.RepairDto;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;
import pl.gesieniec.gsmseller.repair.model.RestoreToShopRequest;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairRepository repository;
    private final RepairMapper mapper;
    private final RepairPdfService pdfService;
    private final PhoneStockRepository phoneStockRepository;
    private final LocationRepository locationRepository;
    private final RepairClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<RepairDto> getHistory(Specification<Repair> spec, Pageable pageable) {
        return repository.findAll(spec, pageable)
            .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<RepairDto> getAllRepairs(String requestedLocation) {
        String userLocation = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        if (auth != null && auth.isAuthenticated()) {
            userLocation = userRepository.findByUsername(auth.getName())
                    .map(User::getLocation)
                    .map(LocationEntity::getName)
                    .orElse(null);
            isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }

        final String finalUserLocation = userLocation;
        final boolean finalIsAdmin = isAdmin;

        return repository.findAll().stream()
            .filter(r -> r.getStatus() != RepairStatus.ARCHIWALNA)
            .filter(r -> {
                // Jeśli Admin prosi o konkretną lokalizację (lub puste = wszystkie)
                if (finalIsAdmin) {
                    return requestedLocation == null || requestedLocation.isBlank() || requestedLocation.equals(r.getLocation());
                }
                // Jeśli zwykły użytkownik prosi o lokalizację, może prosić tylko o swoją lub dostanie swoją domyślnie
                String filterLocation = (requestedLocation != null && !requestedLocation.isBlank()) ? requestedLocation : finalUserLocation;
                return filterLocation == null || filterLocation.equals(r.getLocation());
            })
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

    @Transactional
    public byte[] generateHandoverPdf(UUID technicalId) {
        Repair repair = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));
        if (!repair.isForCustomer()) {
            throw new IllegalStateException("Handover documents are only available for customer repairs");
        }
        
        if (List.of(RepairStatus.NAPRAWIONY, RepairStatus.ANULOWANY, RepairStatus.NIE_DO_NAPRAWY).contains(repair.getStatus())) {
            repair.updateStatus(RepairStatus.ARCHIWALNA);
        }
        
        return pdfService.generateRepairHandoverPdf(repair);
    }

    @Transactional
    public RepairDto addRepair(RepairDto dto) {
        String location = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            location = userRepository.findByUsername(auth.getName())
                    .map(User::getLocation)
                    .map(LocationEntity::getName)
                    .orElse(null);
        }

        RepairClient client = null;
        if (dto.getClientTechnicalId() != null) {
            client = clientRepository.findByTechnicalId(dto.getClientTechnicalId())
                .orElseThrow(() -> new RuntimeException("Client not found: " + dto.getClientTechnicalId()));
        } else if (dto.isAnonymous()) {
            // Anonymous client, only phone number is required
            client = RepairClient.create("Klient", "Anonimowy", dto.getClientPhoneNumber());
            client = clientRepository.save(client);
        } else if (dto.getClientName() != null && !dto.getClientName().isBlank()) {
            client = RepairClient.create(dto.getClientName(), dto.getClientSurname(), dto.getClientPhoneNumber());
            client = clientRepository.save(client);
        }

        Repair repair = Repair.create(
            client,
            dto.getManufacturer(),
            dto.getModel(),
            dto.getImei(),
            dto.getDeviceType(),
            dto.getDeviceCondition(),
            dto.getProblemDescription(),
            dto.getRemarks(),
            dto.isMoistureTraces(),
            dto.isWarrantyRepair(),
            dto.isTurnsOn(),
            dto.isAnonymous(),
            dto.getLockCode(),
            dto.getReceiptDate(),
            dto.getEstimatedRepairDate(),
            dto.getEstimatedCost(),
            dto.getAdvancePayment(),
            dto.getPhotoUrls(),
            dto.getPhoneTechnicalId(),
            dto.getPurchasePrice(),
            dto.getRepairPrice(),
            generateBusinessId(),
            location
        );
        Repair saved = repository.save(repair);
        return mapper.toDto(saved);
    }

    private String generateBusinessId() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfYear = now.with(java.time.temporal.TemporalAdjusters.firstDayOfYear()).withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.time.LocalDateTime endOfYear = now.with(java.time.temporal.TemporalAdjusters.lastDayOfYear()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        long count = repository.countByCreateDateTimeBetween(startOfYear, endOfYear);
        return String.format("RMA/%d/%d", count + 1, now.getYear());
    }

    public String getNextBusinessId() {
        return generateBusinessId();
    }

    @Transactional
    public RepairDto updateRepair(UUID technicalId, RepairDto dto) {
        Repair repair = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Repair not found: " + technicalId));

        RepairClient client = null;
        if (dto.getClientTechnicalId() != null) {
            client = clientRepository.findByTechnicalId(dto.getClientTechnicalId())
                .orElseThrow(() -> new RuntimeException("Client not found: " + dto.getClientTechnicalId()));
        } else if (dto.getClientName() != null && !dto.getClientName().isBlank()) {
            client = RepairClient.create(dto.getClientName(), dto.getClientSurname(), dto.getClientPhoneNumber());
            client = clientRepository.save(client);
        }

        String location = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            location = dto.getLocation();
        }

        repair.update(
            client,
            dto.getManufacturer(),
            dto.getModel(),
            dto.getImei(),
            dto.getDeviceType(),
            dto.getDeviceCondition(),
            dto.getProblemDescription(),
            dto.getRemarks(),
            dto.isMoistureTraces(),
            dto.isWarrantyRepair(),
            dto.isTurnsOn(),
            dto.isAnonymous(),
            dto.getLockCode(),
            dto.getReceiptDate(),
            dto.getEstimatedRepairDate(),
            dto.getEstimatedCost(),
            dto.getAdvancePayment(),
            dto.getPhotoUrls(),
            dto.getPhoneTechnicalId(),
            dto.getPurchasePrice(),
            dto.getRepairPrice(),
            location
        );

        if (dto.getStatus() != null) {
            repair.updateStatus(dto.getStatus());
        }

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

        BigDecimal currentPurchasePrice = phone.getPurchasePrice() != null ? phone.getPurchasePrice() : BigDecimal.ZERO;
        BigDecimal repairPrice = request.getRepairPrice() != null ? request.getRepairPrice() : BigDecimal.ZERO;
        BigDecimal newPurchasePrice = currentPurchasePrice.add(repairPrice);

        phone.update(
            null, // model
            null, // ram
            null, // memory
            null, // color
            null, // imei
            null, // name
            null, // source
            request.getSellingPrice(),
            newPurchasePrice,
            null, // description
            null, // isUsed
            null, // batteryCondition
            null  // comment
        );

        repository.save(repair);
        phoneStockRepository.save(phone);
        repair.updateStatus(RepairStatus.ARCHIWALNA);
    }
}
