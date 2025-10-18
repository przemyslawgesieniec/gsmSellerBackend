package pl.gesieniec.gsmseller.receipt;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;

public interface ReceiptRepository  extends JpaRepository<ReceiptEntity, Long> {

    Optional<ReceiptEntity> findByTechnicalId(UUID technicalId);
}
