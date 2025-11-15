package pl.gesieniec.gsmseller.phone.stock;

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

    public static Specification<PhoneStock> hasImei(String imei) {
        return (root, query, cb) -> {
            if (imei == null || imei.isBlank()) {
                return null;
            }

            // szukaj po pełnym IMEI lub końcówce
            return cb.like(root.get("imei"), "%" + imei);
        };
    }
}