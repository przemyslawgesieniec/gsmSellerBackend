package pl.gesieniec.gsmseller.receipt.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReceiptEditRequest(
    @NotNull LocalDate generateDate,
    @NotNull LocalDate sellDate,
    @NotEmpty List<@NotNull @Valid EditedItem> items
) {
    public record EditedItem(
        @NotNull Long id,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal nettAmount,
        @NotNull @Pattern(regexp = "ZW|0%|5%|8%|23%") String vatRate
    ) {}
}
