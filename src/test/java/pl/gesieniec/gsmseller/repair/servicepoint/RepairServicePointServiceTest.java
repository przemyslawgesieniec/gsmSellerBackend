package pl.gesieniec.gsmseller.repair.servicepoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepairServicePointServiceTest {

    @Mock
    private RepairServicePointRepository repository;

    @InjectMocks
    private RepairServicePointService service;

    @Test
    void shouldReuseExistingPointIgnoringCase() {
        RepairServicePoint existing = RepairServicePoint.create("Serwis Centrum");
        when(repository.findByNameIgnoreCase("serwis centrum")).thenReturn(Optional.of(existing));

        RepairServicePoint result = service.resolve(null, "  serwis   centrum ");

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldCreateNormalizedPointWhenItDoesNotExist() {
        when(repository.findByNameIgnoreCase("Nowy Serwis")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RepairServicePoint result = service.resolve(null, "  Nowy   Serwis ");

        assertThat(result.getTechnicalId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Nowy Serwis");
    }

    @Test
    void shouldRequirePointNameOrTechnicalId() {
        assertThatThrownBy(() -> service.resolve(null, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Wybierz lub wpisz punkt serwisowy");
    }

    @Test
    void shouldResolvePointByTechnicalId() {
        UUID technicalId = UUID.randomUUID();
        RepairServicePoint existing = RepairServicePoint.create("Serwis Zachód");
        when(repository.findByTechnicalId(technicalId)).thenReturn(Optional.of(existing));

        RepairServicePoint result = service.resolve(technicalId, null);

        assertThat(result).isSameAs(existing);
    }
}
