package pl.gesieniec.gsmseller.receipt;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptItemEntity;

public class ReceiptSpecifications {

    public static Specification<ReceiptEntity> createdBy(String username) {
        return (root, query, cb) ->
            username == null || username.isBlank()
                ? null
                : cb.equal(root.get("createdBy"), username);
    }

    public static Specification<ReceiptEntity> hasNumberLike(String invoiceNumber) {
        return (root, query, cb) -> {
            if (invoiceNumber == null || invoiceNumber.isBlank()) {
                return null;
            }

            String pattern = "%" + invoiceNumber.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("number")), pattern);
        };
    }

    public static Specification<ReceiptEntity> hasPhoneImeiLike(String imei) {
        return (root, query, cb) -> {
            if (imei == null || imei.isBlank()) {
                return null;
            }

            query.distinct(true);

            Join<ReceiptEntity, ReceiptItemEntity> itemJoin = root.join("items", JoinType.INNER);

            Subquery<UUID> phoneTechnicalIds = query.subquery(UUID.class);
            Root<PhoneStock> phone = phoneTechnicalIds.from(PhoneStock.class);
            phoneTechnicalIds.select(phone.get("technicalId"))
                .where(cb.like(cb.lower(phone.get("imei")), "%" + imei.trim().toLowerCase() + "%"));

            return itemJoin.get("technicalId").in(phoneTechnicalIds);
        };
    }

    public static Specification<ReceiptEntity> sellDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }

            Path<LocalDate> sellDate = root.get("dateAndPlace").get("sellDate");

            if (from != null && to != null) {
                return cb.between(sellDate, from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(sellDate, from);
            }
            return cb.lessThanOrEqualTo(sellDate, to);
        };
    }
}
