package pl.gesieniec.gsmseller.phone.stock;

import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

public class PhoneStockSpecifications {

    public static Specification<PhoneStock> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }

            String pattern = "%" + name.trim().toLowerCase() + "%";

            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    public static Specification<PhoneStock> hasModel(String model) {
        return (root, query, cb) ->
            model == null ? null : cb.like(cb.lower(root.get("model")), "%" + model.toLowerCase() + "%");
    }

    public static Specification<PhoneStock> hasImeiLike(String imei) {
        return (root, query, cb) -> {
            if (imei == null || imei.isBlank()) {
                return null;
            }

            // szukaj po pełnym IMEI lub końcówce
            return cb.like(root.get("imei"), "%" + imei);
        };
    }

    public static Specification<PhoneStock> hasColor(String color) {
        return (root, query, cb) ->
            color == null || color.isBlank()
                ? null
                : cb.like(cb.lower(root.get("color")), "%" + color.toLowerCase() + "%");
    }

    public static Specification<PhoneStock> hasPriceMin(BigDecimal min) {
        return (root, query, cb) ->
            min == null ? null : cb.greaterThanOrEqualTo(root.get("sellingPrice"), min);
    }

    public static Specification<PhoneStock> hasPriceMax(BigDecimal max) {
        return (root, query, cb) ->
            max == null ? null : cb.lessThanOrEqualTo(root.get("sellingPrice"), max);
    }

    public static Specification<PhoneStock> hasStatus(Status status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<PhoneStock> hasLocationName(String locationName) {
        return (root, query, cb) -> {
            if (locationName == null || locationName.isBlank()) {
                return null;
            }

            return cb.like(
                cb.lower(root.join("location", JoinType.LEFT).get("name")),
                "%" + locationName.toLowerCase() + "%"
            );
        };
    }

}