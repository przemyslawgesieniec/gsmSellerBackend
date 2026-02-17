package pl.gesieniec.gsmseller.receipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;

public interface ReceiptRepository  extends JpaRepository<ReceiptEntity, Long> {

    Optional<ReceiptEntity> findByTechnicalId(UUID technicalId);

    Optional<ReceiptEntity> findFirstByOrderByCreateDateDesc();

    Page<ReceiptEntity> findByCreatedByOrderByCreateDateDesc(String createdBy, Pageable pageable);

    @Query("SELECT SUM(i.grossAmount) FROM ReceiptEntity r JOIN r.items i WHERE i.itemType = 'MISC' AND r.createDate BETWEEN :from AND :to AND r.status = 'AKTYWNA'")
    BigDecimal sumMiscGrossAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

}
