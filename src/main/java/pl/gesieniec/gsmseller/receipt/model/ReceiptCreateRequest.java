package pl.gesieniec.gsmseller.receipt.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReceiptCreateRequest {

    private LocalDate sellDate;

    @NotNull(message = "Stawka VAT jest wymagana")
    private String vatRate;

    @NotEmpty(message = "Lista przedmiotów nie może być pusta")
    private List<ItemRequest> items;

    private String customerNote;
}
