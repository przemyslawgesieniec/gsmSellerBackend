package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.gesieniec.gsmseller.phone.stock.model.Status;

@Repository
public interface PhoneStockRepository extends
    JpaRepository<PhoneStock, Long>,
    JpaSpecificationExecutor<PhoneStock> {

    Optional<PhoneStock> findByTechnicalId(UUID technicalId);

    long countByStatusIn(List<Status> statuses);

    @Query("""
    select p.imei
    from PhoneStock p
    where p.imei in :imeis
      and p.status in :statuses
""")
    Set<String> findImeisByStatusIn(
        @Param("imeis") Collection<String> imeis,
        @Param("statuses") Collection<Status> statuses
    );


    @Query("""
        select coalesce(sum(p.purchasePrice), 0)
        from PhoneStock p
        where p.status in :statuses
    """)
    BigDecimal sumPurchasePriceByStatusIn(List<Status> statuses);

    @Query("""
        select coalesce(sum(p.sellingPrice - p.purchasePrice), 0)
        from PhoneStock p
        where p.status in :statuses
    """)
    BigDecimal sumPotentialProfitByStatusIn(List<Status> statuses);

    long countByStatusInAndSoldAtBetween(
        List<Status> statuses,
        LocalDateTime from,
        LocalDateTime to
    );

    @Query("""
    select coalesce(sum(p.soldFor), 0)
    from PhoneStock p
    where p.status in :statuses
      and p.soldAt between :from and :to
""")
    BigDecimal sumSoldForBetween(
        List<Status> statuses,
        LocalDateTime from,
        LocalDateTime to
    );

    @Query("""
    select coalesce(sum(p.soldFor - p.purchasePrice), 0)
    from PhoneStock p
    where p.status in :statuses
      and p.soldAt between :from and :to
""")
    BigDecimal sumProfitBetween(
        List<Status> statuses,
        LocalDateTime from,
        LocalDateTime to
    );

}
