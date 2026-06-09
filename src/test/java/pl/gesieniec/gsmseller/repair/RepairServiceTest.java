package pl.gesieniec.gsmseller.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.gesieniec.gsmseller.location.LocationRepository;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.repair.client.RepairClientRepository;
import pl.gesieniec.gsmseller.repair.model.RepairDto;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;
import pl.gesieniec.gsmseller.repair.servicepoint.RepairServicePoint;
import pl.gesieniec.gsmseller.repair.servicepoint.RepairServicePointService;
import pl.gesieniec.gsmseller.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

    @Mock private RepairRepository repository;
    @Mock private RepairMapper mapper;
    @Mock private RepairPdfService pdfService;
    @Mock private RepairHandoverPdfService handoverPdfService;
    @Mock private PhoneStockRepository phoneStockRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private RepairClientRepository clientRepository;
    @Mock private RepairServicePointService servicePointService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RepairService service;

    @Test
    void shouldAssignServicePointWhenMovingRepairToInProgress() {
        Repair repair = Repair.create(
            null, "Apple", "iPhone 15", null, "TELEFON", null,
            null, null, null, false, false, true, false,
            null, null, null, null, null, null, null, null,
            "RMA/1/2026", "Warszawa", null
        );
        UUID servicePointTechnicalId = UUID.randomUUID();
        RepairServicePoint servicePoint = RepairServicePoint.create("Serwis Centrum");
        RepairDto mapped = RepairDto.builder().status(RepairStatus.W_NAPRAWIE).build();

        when(repository.findByTechnicalId(repair.getTechnicalId())).thenReturn(Optional.of(repair));
        when(servicePointService.resolve(servicePointTechnicalId, null)).thenReturn(servicePoint);
        when(mapper.toDto(repair)).thenReturn(mapped);

        RepairDto result = service.updateStatus(
            repair.getTechnicalId(),
            RepairStatus.W_NAPRAWIE,
            servicePointTechnicalId,
            null
        );

        assertThat(repair.getStatus()).isEqualTo(RepairStatus.W_NAPRAWIE);
        assertThat(repair.getServicePoint()).isSameAs(servicePoint);
        assertThat(result).isSameAs(mapped);
        verify(servicePointService).resolve(servicePointTechnicalId, null);
    }
}
