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
    
    @org.springframework.data.jpa.repository.Query("SELECT r.businessId FROM Repair r WHERE r.businessId LIKE %:year AND r.businessId LIKE 'RMA/%' ORDER BY r.id DESC")
    java.util.List<String> findBusinessIdsByYear(@org.springframework.data.repository.query.Param("year") int year);

    java.util.List<Repair> findAllByArchivedAndCreateDateTimeBetween(boolean archived, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
