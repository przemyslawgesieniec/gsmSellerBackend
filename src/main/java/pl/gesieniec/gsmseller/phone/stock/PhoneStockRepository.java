package pl.gesieniec.gsmseller.phone.stock;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneStockRepository extends JpaRepository<PhoneStock, Long>,
    JpaSpecificationExecutor<PhoneStock> {

    Optional<PhoneStock> findByTechnicalId(UUID technicalId);
}
