package pl.gesieniec.gsmseller.offer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {
    @EntityGraph(attributePaths = {"phoneStock", "phoneStock.location", "phoneStock.phoneModel"})
    Optional<Offer> findByPhoneStockTechnicalId(UUID technicalId);

    @EntityGraph(attributePaths = {"phoneStock", "phoneStock.phoneModel"})
    List<Offer> findAllByPhoneStockPhoneModelTechnicalId(UUID technicalId);

    @Override
    @EntityGraph(attributePaths = {"phoneStock", "phoneStock.location", "phoneStock.phoneModel"})
    Page<Offer> findAll(Specification<Offer> spec, Pageable pageable);
}
