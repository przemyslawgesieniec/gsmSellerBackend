package pl.gesieniec.gsmseller.offer;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pl.gesieniec.gsmseller.phone.model.PhoneModels;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfferSpecifications {

    private static final String OTHER_BRANDS_FILTER = "__OTHER__";

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

            List<Expression<String>> brandFields = List.of(
                root.get("brand"),
                phoneStockJoin.get("name"),
                phoneStockJoin.get("model"),
                phoneModelJoin.get("brand"),
                phoneModelJoin.get("model")
            );

            String normalizedBrand = brand.trim().toLowerCase();
            if (OTHER_BRANDS_FILTER.equalsIgnoreCase(normalizedBrand)) {
                return cb.not(cb.or(
                    matchesBrandGroup(cb, brandFields, "apple"),
                    matchesBrandGroup(cb, brandFields, "samsung"),
                    matchesBrandGroup(cb, brandFields, "xiaomi"),
                    matchesBrandGroup(cb, brandFields, "redmi")
                ));
            }

            return matchesBrandGroup(cb, brandFields, normalizedBrand);
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
            String modelValue = model.trim().toLowerCase();
            String pattern = "%" + modelValue + "%";
            String normalizedPattern = "%" + normalize(modelValue) + "%";
            Expression<String> phoneModelDisplayName = cb.concat(
                cb.concat(cb.coalesce(phoneModelJoin.get("brand"), ""), " "),
                cb.coalesce(phoneModelJoin.get("model"), "")
            );

            return cb.or(
                cb.like(cb.lower(phoneStockJoin.get("name")), pattern),
                cb.like(cb.lower(phoneStockJoin.get("model")), pattern),
                cb.like(cb.lower(phoneModelJoin.get("model")), pattern),
                cb.like(cb.lower(phoneModelDisplayName), pattern),
                cb.like(normalizedExpression(cb, phoneStockJoin.get("name")), normalizedPattern),
                cb.like(normalizedExpression(cb, phoneStockJoin.get("model")), normalizedPattern),
                cb.like(normalizedExpression(cb, phoneModelJoin.get("model")), normalizedPattern),
                cb.like(normalizedExpression(cb, phoneModelDisplayName), normalizedPattern)
            );
        };
    }

    public static Specification<Offer> hasPhoneModelTechnicalId(UUID phoneModelTechnicalId) {
        return (root, query, cb) -> {
            if (phoneModelTechnicalId == null) {
                return null;
            }

            Join<Offer, PhoneStock> phoneStockJoin = root.join("phoneStock", JoinType.INNER);
            Join<PhoneStock, PhoneModels> phoneModelJoin = phoneStockJoin.join("phoneModel", JoinType.INNER);
            return cb.equal(phoneModelJoin.get("technicalId"), phoneModelTechnicalId);
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

    private static Predicate matchesBrandGroup(CriteriaBuilder cb, List<Expression<String>> fields, String brand) {
        String normalizedBrand = brand.trim().toLowerCase();
        List<Predicate> predicates = new ArrayList<>();

        if ("apple".equals(normalizedBrand)) {
            predicates.add(matchesAnyField(cb, fields, "apple"));
            predicates.add(matchesAnyField(cb, fields, "iphone"));
            return cb.or(predicates.toArray(Predicate[]::new));
        }

        if ("xiaomi".equals(normalizedBrand)) {
            return cb.and(
                matchesAnyField(cb, fields, "xiaomi"),
                cb.not(matchesAnyField(cb, fields, "redmi"))
            );
        }

        return matchesAnyField(cb, fields, normalizedBrand);
    }

    private static Predicate matchesAnyField(CriteriaBuilder cb, List<Expression<String>> fields, String value) {
        String pattern = "%" + value.trim().toLowerCase() + "%";
        return cb.or(fields.stream()
            .map(field -> cb.like(cb.lower(cb.coalesce(field, "")), pattern))
            .toArray(Predicate[]::new));
    }

    private static Expression<String> normalizedExpression(CriteriaBuilder cb, Expression<String> expression) {
        Expression<String> lower = cb.lower(cb.coalesce(expression, ""));
        Expression<String> withoutSpaces = cb.function("replace", String.class, lower, cb.literal(" "), cb.literal(""));
        return cb.function("replace", String.class, withoutSpaces, cb.literal("-"), cb.literal(""));
    }

    private static String normalize(String value) {
        return value.replace(" ", "").replace("-", "");
    }
}
