package pl.gesieniec.gsmseller.repair;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RepairNoteRepository extends JpaRepository<RepairNote, Long> {
    List<RepairNote> findAllByRepairOrderByCreatedAtDesc(Repair repair);
}
