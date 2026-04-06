package pl.gesieniec.gsmseller.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchasePhotoRepository extends JpaRepository<PurchasePhoto, Long> {
    Optional<PurchasePhoto> findByTechnicalId(UUID technicalId);
}
