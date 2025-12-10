package pl.gesieniec.gsmseller.receipt;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;

public interface ReceiptRepository  extends JpaRepository<ReceiptEntity, Long> {

    Optional<ReceiptEntity> findByTechnicalId(UUID technicalId);

    Optional<ReceiptEntity> findFirstByOrderByCreateDateDesc();

    Page<ReceiptEntity> findByCreatedByOrderByCreateDateDesc(String createdBy, Pageable pageable);


}
