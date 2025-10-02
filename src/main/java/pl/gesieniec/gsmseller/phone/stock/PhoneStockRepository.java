package pl.gesieniec.gsmseller.phone.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneStockRepository extends JpaRepository<PhoneStock, Long> {

}
