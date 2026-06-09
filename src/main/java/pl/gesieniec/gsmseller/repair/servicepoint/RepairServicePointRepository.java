package pl.gesieniec.gsmseller.repair.servicepoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairServicePointRepository extends JpaRepository<RepairServicePoint, Long> {
    Optional<RepairServicePoint> findByTechnicalId(UUID technicalId);
    Optional<RepairServicePoint> findByNameIgnoreCase(String name);
    List<RepairServicePoint> findTop20ByNameContainingIgnoreCaseOrderByNameAsc(String query);
    List<RepairServicePoint> findTop20ByOrderByNameAsc();
}
