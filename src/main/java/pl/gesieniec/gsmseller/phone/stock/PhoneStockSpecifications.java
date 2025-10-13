package pl.gesieniec.gsmseller.phone.stock;

import org.springframework.data.jpa.domain.Specification;

public class PhoneStockSpecifications {

    public static Specification<PhoneStock> hasName(String name) {
        return (root, query, cb) ->
            name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<PhoneStock> hasModel(String model) {
        return (root, query, cb) ->
            model == null ? null : cb.like(cb.lower(root.get("model")), "%" + model.toLowerCase() + "%");
    }

    public static Specification<PhoneStock> hasImei1(String imei1) {
        return (root, query, cb) ->
            imei1 == null ? null : cb.equal(root.get("imei1"), imei1);
    }

    public static Specification<PhoneStock> hasImei2(String imei2) {
        return (root, query, cb) ->
            imei2 == null ? null : cb.equal(root.get("imei2"), imei2);
    }
}