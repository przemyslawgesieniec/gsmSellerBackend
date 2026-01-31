package pl.gesieniec.gsmseller.repair.client;

import org.springframework.data.jpa.domain.Specification;

public class RepairClientSpecifications {

    public static Specification<RepairClient> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<RepairClient> hasSurname(String surname) {
        return (root, query, cb) -> {
            if (surname == null || surname.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
        };
    }

    public static Specification<RepairClient> hasPhoneNumber(String phoneNumber) {
        return (root, query, cb) -> {
            if (phoneNumber == null || phoneNumber.isBlank()) {
                return null;
            }
            return cb.like(root.get("phoneNumber"), "%" + phoneNumber + "%");
        };
    }

    public static Specification<RepairClient> searchByAny(String query) {
        return (root, q, cb) -> {
            if (query == null || query.isBlank()) {
                return null;
            }
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("surname")), pattern),
                cb.like(root.get("phoneNumber"), pattern)
            );
        };
    }
}
