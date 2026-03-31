package pl.gesieniec.gsmseller.offer;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;

import java.math.BigDecimal;

public class OfferSpecifications {

    public static Specification<Offer> hasBrand(String brand) {
        return (root, query, cb) -> {
            if (brand == null || brand.isBlank() || "all".equalsIgnoreCase(brand)) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            // PhoneStock.name jest używane jako marka (brand) w OfferService.mapToDto
            return cb.equal(phoneStockJoin.get("name"), brand);
        };
    }

    public static Specification<Offer> hasModel(String model) {
        return (root, query, cb) -> {
            if (model == null || model.isBlank() || "all".equalsIgnoreCase(model)) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            return cb.like(cb.lower(phoneStockJoin.get("model")), "%" + model.toLowerCase() + "%");
        };
    }

    public static Specification<Offer> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            // Jeśli status na frontendzie to "Nowy"/"Używany", a w bazie mamy pole 'used' (boolean)
            if ("Nowy".equalsIgnoreCase(status)) {
                return cb.isFalse(phoneStockJoin.get("used"));
            } else if ("Używany".equalsIgnoreCase(status) || "Odnowiony".equalsIgnoreCase(status)) {
                // Przyjmuję, że 'Odnowiony' też jest 'used' w sensie technicznym bazy danych, 
                // chyba że istnieje bardziej szczegółowy status w PhoneStock.status
                return cb.isTrue(phoneStockJoin.get("used"));
            }
            return null;
        };
    }

    public static Specification<Offer> hasLocation(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isBlank() || "all".equalsIgnoreCase(location)) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            return cb.equal(phoneStockJoin.join("location").get("name"), location);
        };
    }

    public static Specification<Offer> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            if (minPrice != null && maxPrice != null) {
                return cb.between(phoneStockJoin.get("sellingPrice"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(phoneStockJoin.get("sellingPrice"), minPrice);
            } else if (maxPrice != null) {
                return cb.lessThanOrEqualTo(phoneStockJoin.get("sellingPrice"), maxPrice);
            }
            return null;
        };
    }

    public static Specification<Offer> search(String queryText) {
        return (root, query, cb) -> {
            if (queryText == null || queryText.isBlank()) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            String pattern = "%" + queryText.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(phoneStockJoin.get("name")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("model")), pattern)
            );
        };
    }
}
