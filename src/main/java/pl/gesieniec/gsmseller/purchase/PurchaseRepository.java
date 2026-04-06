package pl.gesieniec.gsmseller.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByTechnicalId(UUID technicalId);

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.photos")
    List<Purchase> findAllWithPhotos();
}
