package pl.gesieniec.gsmseller.repair.servicepoint;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class RepairServicePointService {

    private final RepairServicePointRepository repository;

    @Transactional(readOnly = true)
    public List<RepairServicePointDto> search(String query) {
        List<RepairServicePoint> points = query == null || query.isBlank()
            ? repository.findTop20ByOrderByNameAsc()
            : repository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(query.trim());

        return points.stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public RepairServicePointDto create(String name) {
        return toDto(resolve(null, name));
    }

    @Transactional
    public RepairServicePoint resolve(UUID technicalId, String name) {
        if (technicalId != null) {
            return repository.findByTechnicalId(technicalId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Nie znaleziono punktu serwisowego: " + technicalId));
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Wybierz lub wpisz punkt serwisowy");
        }

        String normalizedName = name.trim().replaceAll("\\s+", " ");
        return repository.findByNameIgnoreCase(normalizedName)
            .orElseGet(() -> repository.save(RepairServicePoint.create(normalizedName)));
    }

    private RepairServicePointDto toDto(RepairServicePoint point) {
        return new RepairServicePointDto(point.getTechnicalId(), point.getName());
    }
}
