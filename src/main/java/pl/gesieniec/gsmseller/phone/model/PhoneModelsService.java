package pl.gesieniec.gsmseller.phone.model;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;
import pl.gesieniec.gsmseller.offer.OfferService;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;

@Service
@RequiredArgsConstructor
public class PhoneModelsService {

    private final PhoneModelsRepository repository;
    private final PhoneModelsMapper mapper;
    private final PhoneStockRepository phoneStockRepository;
    private final OfferService offerService;

    @Transactional(readOnly = true)
    public Page<PhoneModelsDto> getModels(String search, Pageable pageable) {
        Specification<PhoneModels> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("brand")), pattern),
                cb.like(cb.lower(root.get("model")), pattern),
                cb.like(cb.lower(root.get("colors")), pattern),
                cb.like(cb.lower(root.get("memory")), pattern),
                cb.like(cb.lower(root.get("ram")), pattern)
            );
        };

        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getFilterOptionsByBrand() {
        return repository.findAll(Sort.by("brand").ascending().and(Sort.by("model").ascending()))
            .stream()
            .filter(model -> model.getBrand() != null && !model.getBrand().isBlank())
            .filter(model -> model.getModel() != null && !model.getModel().isBlank())
            .collect(Collectors.groupingBy(
                model -> model.getBrand().trim(),
                java.util.TreeMap::new,
                Collectors.mapping(
                    model -> model.getModel().trim(),
                    Collectors.collectingAndThen(
                        Collectors.toCollection(java.util.TreeSet::new),
                        java.util.ArrayList::new
                    )
                )
            ));
    }

    @Transactional(readOnly = true)
    public Map<String, List<PhoneModelFilterOption>> getExternalFilterOptionsByBrand() {
        return repository.findAll(Sort.by("brand").ascending().and(Sort.by("model").ascending()))
            .stream()
            .filter(model -> model.getBrand() != null && !model.getBrand().isBlank())
            .filter(model -> model.getModel() != null && !model.getModel().isBlank())
            .sorted(Comparator
                .comparing(PhoneModels::getBrand, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PhoneModels::getModel, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.groupingBy(
                model -> model.getBrand().trim(),
                java.util.TreeMap::new,
                Collectors.mapping(
                    model -> new PhoneModelFilterOption(
                        model.getTechnicalId(),
                        model.getBrand().trim(),
                        model.getModel().trim(),
                        model.getDisplayName()
                    ),
                    Collectors.toList()
                )
            ));
    }

    @Transactional(readOnly = true)
    public PhoneModelsDto getModel(UUID technicalId) {
        return repository.findByTechnicalId(technicalId)
            .map(mapper::toDto)
            .orElseThrow(() -> new EntityNotFoundException("Phone model not found: " + technicalId));
    }

    @Transactional
    public PhoneModelsDto create(PhoneModelsDto dto) {
        PhoneModels model = new PhoneModels(
            dto.getModel(),
            dto.getScreen(),
            dto.getScreenResolution(),
            dto.getDisplayType(),
            dto.getMemory(),
            dto.getRam(),
            dto.getSimCardType(),
            dto.getPortType(),
            dto.getDualSim(),
            dto.getColors(),
            dto.getFrontCameras(),
            dto.getBackCameras(),
            dto.getBatteryCapacity(),
            dto.getBrand(),
            dto.getDisplayPriority()
        );

        return mapper.toDto(repository.save(model));
    }

    @Transactional
    public PhoneModelsDto update(UUID technicalId, PhoneModelsDto dto) {
        PhoneModels model = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Phone model not found: " + technicalId));

        model.update(
            dto.getModel(),
            dto.getScreen(),
            dto.getScreenResolution(),
            dto.getDisplayType(),
            dto.getMemory(),
            dto.getRam(),
            dto.getSimCardType(),
            dto.getPortType(),
            dto.getDualSim(),
            dto.getColors(),
            dto.getFrontCameras(),
            dto.getBackCameras(),
            dto.getBatteryCapacity(),
            dto.getBrand(),
            dto.getDisplayPriority()
        );

        offerService.refreshOffersForPhoneModel(model);

        return mapper.toDto(model);
    }

    @Transactional
    public void delete(UUID technicalId) {
        PhoneModels model = repository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new EntityNotFoundException("Phone model not found: " + technicalId));

        if (phoneStockRepository.existsByPhoneModelTechnicalId(technicalId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Nie można usunąć modelu przypisanego do telefonu"
            );
        }

        repository.delete(model);
    }
}
