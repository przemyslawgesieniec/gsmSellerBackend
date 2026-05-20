package pl.gesieniec.gsmseller.phone.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/phone-models")
public class PhoneModelsController {

    private final PhoneModelsService service;

    @GetMapping
    public Page<PhoneModelsDto> getModels(
        @RequestParam(required = false) String search,
        @PageableDefault(sort = "model", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return service.getModels(search, pageable);
    }

    @GetMapping("/filter-options")
    public Map<String, List<String>> getFilterOptions() {
        return service.getFilterOptionsByBrand();
    }

    @GetMapping("/{technicalId}")
    public PhoneModelsDto getModel(@PathVariable UUID technicalId) {
        return service.getModel(technicalId);
    }

    @PostMapping
    public PhoneModelsDto create(@RequestBody PhoneModelsDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{technicalId}")
    public PhoneModelsDto update(@PathVariable UUID technicalId, @RequestBody PhoneModelsDto dto) {
        return service.update(technicalId, dto);
    }

    @DeleteMapping("/{technicalId}")
    public ResponseEntity<Void> delete(@PathVariable UUID technicalId) {
        service.delete(technicalId);
        return ResponseEntity.noContent().build();
    }
}
