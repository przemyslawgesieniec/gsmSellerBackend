package pl.gesieniec.gsmseller.repair;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepairNoteRepository extends JpaRepository<RepairNote, Long> {
    @Query("SELECT n.repair.technicalId FROM RepairNote n WHERE n.id = :noteId")
    Optional<UUID> findRepairTechnicalId(@Param("noteId") Long noteId);

    List<RepairNote> findAllByRepairOrderByCreatedAtDesc(Repair repair);
}
