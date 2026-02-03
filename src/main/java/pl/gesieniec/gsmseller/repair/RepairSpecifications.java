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

    public static Specification<Repair> hasClientNameOrPhone(String query) {
        return (root, q, cb) -> {
            if (query == null || query.isBlank()) return null;
            
            Join<Repair, RepairClient> clientJoin = root.join("client");

            // Obsługa formatu z autocomplete: Imię Nazwisko (Telefon)
            if (query.contains("(") && query.contains(")")) {
                String phone = query.substring(query.lastIndexOf("(") + 1, query.lastIndexOf(")"));
                if (!phone.isBlank()) {
                    return cb.equal(clientJoin.get("phoneNumber"), phone);
                }
            }

            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(clientJoin.get("name")), pattern),
                cb.like(cb.lower(clientJoin.get("surname")), pattern),
                cb.like(clientJoin.get("phoneNumber"), pattern)
            );
        };
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
