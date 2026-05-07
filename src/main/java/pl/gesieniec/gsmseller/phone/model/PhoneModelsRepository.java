package pl.gesieniec.gsmseller.phone.model;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneModelsRepository extends
    JpaRepository<PhoneModels, Long>,
    JpaSpecificationExecutor<PhoneModels> {

    Optional<PhoneModels> findByTechnicalId(UUID technicalId);
}
