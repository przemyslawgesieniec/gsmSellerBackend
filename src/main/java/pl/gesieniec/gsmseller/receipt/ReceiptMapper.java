package pl.gesieniec.gsmseller.receipt;

import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.gesieniec.gsmseller.receipt.entity.DateAndPlaceEntity;
import pl.gesieniec.gsmseller.receipt.entity.ItemEntity;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptEntity;
import pl.gesieniec.gsmseller.receipt.entity.SellerEntity;
import pl.gesieniec.gsmseller.receipt.model.DateAndPlace;
import pl.gesieniec.gsmseller.receipt.model.Item;
import pl.gesieniec.gsmseller.receipt.model.Receipt;
import pl.gesieniec.gsmseller.receipt.model.Seller;
import pl.gesieniec.gsmseller.receipt.model.VatRate;

@Mapper(componentModel = "spring")
public interface ReceiptMapper {

    @Mapping(source = "items", target = "items")
    Receipt toModel(ReceiptEntity entity);

    @Mapping(source = "items", target = "items")
    ReceiptEntity toEntity(Receipt dto);

    Seller toModel(SellerEntity entity);
    SellerEntity toEntity(Seller dto);

    DateAndPlace toModel(DateAndPlaceEntity entity);
    DateAndPlaceEntity toEntity(DateAndPlace dto);

    Item toModel(ItemEntity entity);
    ItemEntity toEntity(Item dto);

    default VatRate map(BigDecimal value) {
        if (value == null) {
            return null;
        }

        if (value.compareTo(BigDecimal.valueOf(0.23)) == 0) return VatRate.VAT_23;
        if (value.compareTo(BigDecimal.valueOf(0.08)) == 0) return VatRate.VAT_8;
        if (value.compareTo(BigDecimal.valueOf(0.05)) == 0) return VatRate.VAT_5;
        if (value.compareTo(BigDecimal.ZERO) == 0) return VatRate.VAT_0;

        return VatRate.VAT_EXEMPT;
    }

    default BigDecimal map(VatRate vatRate) {
        return vatRate != null ? vatRate.getValue() : null;
    }
}
