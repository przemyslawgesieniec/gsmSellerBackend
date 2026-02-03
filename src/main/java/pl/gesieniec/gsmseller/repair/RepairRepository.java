package pl.gesieniec.gsmseller.repair;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairRepository extends JpaRepository<Repair, Long>, JpaSpecificationExecutor<Repair> {
    Optional<Repair> findByTechnicalId(UUID technicalId);

    long countByCreateDateTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
