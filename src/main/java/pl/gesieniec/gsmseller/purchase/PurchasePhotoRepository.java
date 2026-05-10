package pl.gesieniec.gsmseller.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchasePhotoRepository extends JpaRepository<PurchasePhoto, Long> {
    Optional<PurchasePhoto> findByTechnicalId(UUID technicalId);

    @Query("SELECT photo.technicalId FROM Purchase purchase JOIN purchase.photos photo WHERE purchase.technicalId = :purchaseTechnicalId")
    List<UUID> findTechnicalIdsByPurchaseTechnicalId(@Param("purchaseTechnicalId") UUID purchaseTechnicalId);
}
