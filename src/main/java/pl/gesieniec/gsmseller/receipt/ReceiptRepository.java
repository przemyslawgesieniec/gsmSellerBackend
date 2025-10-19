package pl.gesieniec.gsmseller.receipt;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;

public interface ReceiptRepository  extends JpaRepository<ReceiptEntity, Long> {

    Optional<ReceiptEntity> findByTechnicalId(UUID technicalId);

    @Query(value = "SELECT number " +
        "FROM receipt_entity " +
        "WHERE EXTRACT(YEAR FROM create_date) = EXTRACT(YEAR FROM CURRENT_DATE) " +
        "  AND EXTRACT(MONTH FROM create_date) = EXTRACT(MONTH FROM CURRENT_DATE) " +
        "ORDER BY create_date DESC " +
        "LIMIT 1", nativeQuery = true)
    Optional<String> getLastReceiptNumber();
}
