package pl.gesieniec.gsmseller.repair.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.repair.model.RepairClientDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairClientService {

    private final RepairClientRepository repository;
    private final RepairClientMapper mapper;

    public Page<RepairClientDto> getClients(String name, String surname, String phoneNumber, int page, int size, String sortBy, String sortDir) {
        Specification<RepairClient> spec = Specification
            .where(RepairClientSpecifications.hasName(name))
            .and(RepairClientSpecifications.hasSurname(surname))
            .and(RepairClientSpecifications.hasPhoneNumber(phoneNumber));

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return repository.findAll(spec, pageable)
            .map(mapper::toDto);
    }

    public Page<RepairClientDto> searchClients(String query, int page, int size) {
        Specification<RepairClient> spec = RepairClientSpecifications.searchByAny(query);
        Pageable pageable = PageRequest.of(page, size, Sort.by("surname").ascending());

        return repository.findAll(spec, pageable)
            .map(mapper::toDto);
    }

    @Transactional
    public RepairClientDto createClient(RepairClientDto dto) {
        log.info("Tworzenie nowego klienta: {} {}", dto.getName(), dto.getSurname());
        RepairClient client = RepairClient.create(dto.getName(), dto.getSurname(), dto.getPhoneNumber());
        RepairClient saved = repository.save(client);
        return mapper.toDto(saved);
    }
}
