package pl.gesieniec.gsmseller.repair;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairRepository extends JpaRepository<Repair, Long>, JpaSpecificationExecutor<Repair> {
    Optional<Repair> findByTechnicalId(UUID technicalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Repair r WHERE r.technicalId = :technicalId")
    Optional<Repair> findForUpdate(@Param("technicalId") UUID technicalId);

    long countByCreateDateTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT r.businessId FROM Repair r WHERE r.businessId LIKE concat('%/', :year) AND r.businessId LIKE 'RMA/%'
        UNION ALL
        SELECT e.details FROM EventLogEntry e WHERE e.deletedRepairSnapshot IS NOT NULL
            AND e.details LIKE concat('%/', :year) AND e.details LIKE 'RMA/%'
        """)
    List<String> findBusinessIdsByYear(@Param("year") String year);

    List<Repair> findAllByArchivedAndHandoverDateBetween(boolean archived, LocalDateTime start, LocalDateTime end);
}
