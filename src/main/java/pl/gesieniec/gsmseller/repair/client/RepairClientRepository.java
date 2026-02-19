package pl.gesieniec.gsmseller.repair.client;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairClientRepository extends JpaRepository<RepairClient, Long>, JpaSpecificationExecutor<RepairClient> {
    Optional<RepairClient> findByTechnicalId(UUID technicalId);
}
