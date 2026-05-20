package pl.gesieniec.gsmseller.offer;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import pl.gesieniec.gsmseller.phone.model.PhoneModels;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;

import java.math.BigDecimal;

public class OfferSpecifications {

    public static Specification<Offer> orderByModelDisplayPriority() {
        return (root, query, cb) -> {
            if (!isCountQuery(query.getResultType())) {
                Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.LEFT);
                Join<PhoneStock, PhoneModels> phoneModelJoin = phoneStockJoin.join("phoneModel", JoinType.LEFT);

                query.orderBy(
                    cb.desc(cb.coalesce(phoneModelJoin.get("displayPriority"), 0)),
                    cb.desc(root.get("id"))
                );
            }
            return null;
        };
    }

    public static Specification<Offer> hasBrand(String brand) {
        return (root, query, cb) -> {
            if (brand == null || brand.isBlank() || "all".equalsIgnoreCase(brand)) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            Join<PhoneStock, PhoneModels> phoneModelJoin = phoneStockJoin.join("phoneModel", JoinType.LEFT);
            String pattern = "%" + brand.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("brand")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("model")), pattern),
                cb.like(cb.lower(phoneModelJoin.get("brand")), pattern)
            );
        };
    }

    public static Specification<Offer> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank() || "all".equalsIgnoreCase(name)) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            String pattern = "%" + name.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("brand")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("name")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("model")), pattern)
            );
        };
    }

    public static Specification<Offer> hasModel(String model) {
        return (root, query, cb) -> {
            if (model == null || model.isBlank() || "all".equalsIgnoreCase(model)) {
                return null;
            }

            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            Join<PhoneStock, PhoneModels> phoneModelJoin = phoneStockJoin.join("phoneModel", JoinType.LEFT);
            String pattern = "%" + model.toLowerCase() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("model")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("model")), pattern),
                cb.like(cb.lower(phoneModelJoin.get("model")), pattern)
            );
        };
    }

    public static Specification<Offer> hasImei(String imei) {
        return (root, query, cb) -> {
            if (imei == null || imei.isBlank()) {
                return null;
            }
            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            return cb.like(cb.lower(phoneStockJoin.get("imei")), "%" + imei.toLowerCase() + "%");
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
            if ("__ONLINE__".equals(location)) {
                return cb.isNull(phoneStockJoin.get("location"));
            }
            return cb.equal(phoneStockJoin.join("location", JoinType.LEFT).get("name"), location);
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
                cb.like(cb.lower(root.get("brand")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("name")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("model")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("imei")), pattern)
            );
        };
    }

    private static boolean isCountQuery(Class<?> resultType) {
        return resultType == Long.class || resultType == long.class;
    }
}
