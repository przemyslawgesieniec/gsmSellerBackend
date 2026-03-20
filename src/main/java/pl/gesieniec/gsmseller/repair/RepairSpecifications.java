package pl.gesieniec.gsmseller.repair;

import jakarta.persistence.criteria.Join;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import pl.gesieniec.gsmseller.repair.client.RepairClient;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

public class RepairSpecifications {

    public static Specification<Repair> hasStatus(RepairStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Repair> archived(Boolean archived) {
        return (root, query, cb) -> archived == null ? null : cb.equal(root.get("archived"), archived);
    }

    public static Specification<Repair> hasClientNameOrPhoneOrRmaOrModel(String query) {
        return (root, q, cb) -> {
            if (query == null || query.isBlank()) return null;
            
            Join<Repair, RepairClient> clientJoin = root.join("client", jakarta.persistence.criteria.JoinType.LEFT);

            // Obsługa formatu z autocomplete: Imię Nazwisko (Telefon)
            if (query.contains("(") && query.contains(")")) {
                String phone = query.substring(query.lastIndexOf("(") + 1, query.lastIndexOf(")"));
                if (!phone.isBlank()) {
                    return cb.equal(clientJoin.get("phoneNumber"), phone);
                }
            }

            String pattern = "%" + query.toLowerCase() + "%";
            
            // Wyszukiwanie łączone (producent + model)
            String[] parts = query.toLowerCase().split("\\s+");
            if (parts.length > 1) {
                return cb.or(
                    cb.like(cb.lower(clientJoin.get("name")), pattern),
                    cb.like(cb.lower(clientJoin.get("surname")), pattern),
                    cb.like(clientJoin.get("phoneNumber"), pattern),
                    cb.like(cb.lower(root.get("businessId")), pattern),
                    cb.like(cb.lower(root.get("manufacturer")), pattern),
                    cb.like(cb.lower(root.get("model")), pattern),
                    cb.and(
                        cb.like(cb.lower(root.get("manufacturer")), "%" + parts[0] + "%"),
                        cb.like(cb.lower(root.get("model")), "%" + parts[1] + "%")
                    )
                );
            }

            return cb.or(
                cb.like(cb.lower(clientJoin.get("name")), pattern),
                cb.like(cb.lower(clientJoin.get("surname")), pattern),
                cb.like(clientJoin.get("phoneNumber"), pattern),
                cb.like(cb.lower(root.get("businessId")), pattern),
                cb.like(cb.lower(root.get("manufacturer")), pattern),
                cb.like(cb.lower(root.get("model")), pattern)
            );
        };
    }

    public static Specification<Repair> hasLocation(String location) {
        return (root, query, cb) -> (location == null || location.isBlank()) ? null : cb.equal(root.get("location"), location);
    }

    public static Specification<Repair> receiptDateBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("receiptDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("receiptDate"), from);
            return cb.lessThanOrEqualTo(root.get("receiptDate"), to);
        };
    }

    public static Specification<Repair> handoverDateBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("handoverDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("handoverDate"), from);
            return cb.lessThanOrEqualTo(root.get("handoverDate"), to);
        };
    }
}
