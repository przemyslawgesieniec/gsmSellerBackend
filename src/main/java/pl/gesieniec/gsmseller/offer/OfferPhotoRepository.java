package pl.gesieniec.gsmseller.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferPhotoRepository extends JpaRepository<OfferPhoto, Long> {
    Optional<OfferPhoto> findByTechnicalId(UUID technicalId);
    List<OfferPhoto> findAllByTechnicalIdIn(List<UUID> technicalIds);

    @Query("SELECT p.technicalId FROM OfferPhoto p WHERE p.id IN (SELECT ph.id FROM Offer o JOIN o.photos ph WHERE o.id = :offerId)")
    List<UUID> findPhotoTechnicalIdsByOfferId(@Param("offerId") Long offerId);
}
