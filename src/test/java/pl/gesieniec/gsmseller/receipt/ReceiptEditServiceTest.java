package pl.gesieniec.gsmseller.receipt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import pl.gesieniec.gsmseller.common.ItemType;
import pl.gesieniec.gsmseller.receipt.entity.*;
import pl.gesieniec.gsmseller.receipt.model.*;

class ReceiptEditServiceTest {
    private final ReceiptRepository repository = mock(ReceiptRepository.class);
    private final PdfGenerationService pdfService = mock(PdfGenerationService.class);
    private final ReceiptEditService service = new ReceiptEditService(repository, Mappers.getMapper(ReceiptMapper.class), pdfService);
    private ReceiptEntity entity;
    private ReceiptItemEntity phone;
    private ReceiptItemEntity accessory;
    private final LocalDate originalDate = LocalDate.of(2026, 10, 1);
    private final LocalDate editedDate = LocalDate.of(2026, 10, 20);

    @BeforeEach
    void setUp() {
        phone = new ReceiptItemEntity(10L, "Telefon", new BigDecimal("100.00"), VatRate.VAT_EXEMPT,
            BigDecimal.ZERO, new BigDecimal("100.00"), UUID.randomUUID(), 12, true, ItemType.PHONE, "Notatka");
        accessory = new ReceiptItemEntity(11L, "Etui", new BigDecimal("20.00"), VatRate.VAT_EXEMPT,
            BigDecimal.ZERO, new BigDecimal("20.00"), null, null, null, ItemType.MISC, null);
        entity = new ReceiptEntity(1L, UUID.randomUUID(), originalDate.atTime(12, 30), "001/10/2026",
            new SellerEntity("Sprzedawca", "Ulica", "00-000", "Miasto", "123"),
            new DateAndPlaceEntity("Miasto", originalDate, originalDate), List.of(phone, accessory),
            "seller", ReceiptStatus.AKTYWNA, "Uwagi klienta");
        when(repository.findByTechnicalId(entity.getTechnicalId())).thenReturn(Optional.of(entity));
        when(pdfService.generateReceiptPdf(any())).thenReturn(new byte[] {1, 2, 3});
    }

    private ReceiptEditRequest request() {
        // Deliberately reversed: update by persistent item ID, not request order or phone UUID.
        return new ReceiptEditRequest(editedDate, editedDate.minusDays(1), List.of(
            new ReceiptEditRequest.EditedItem(11L, new BigDecimal("12.10"), "0%"),
            new ReceiptEditRequest.EditedItem(10L, new BigDecimal("200.50"), "23%")));
    }

    @Test
    void downloadOnlyRecalculatesPdfWithoutMutatingAnyStoredData() {
        var dates = entity.getDateAndPlace();
        byte[] pdf = service.editPdf(entity.getTechnicalId(), request(), false);
        assertThat(pdf).containsExactly(1, 2, 3);
        assertThat(entity.getDateAndPlace()).isSameAs(dates);
        assertThat(phone.getNettAmount()).isEqualByComparingTo("100.00");
        assertThat(phone.getVatRate()).isEqualTo(VatRate.VAT_EXEMPT);
        assertThat(accessory.getGrossAmount()).isEqualByComparingTo("20.00");
        verify(repository, never()).save(any());
        ArgumentCaptor<Receipt> captor = ArgumentCaptor.forClass(Receipt.class);
        verify(pdfService).generateReceiptPdf(captor.capture());
        Receipt pdfReceipt = captor.getValue();
        assertThat(pdfReceipt.getDateAndPlace().generateDate()).isEqualTo(editedDate);
        assertThat(pdfReceipt.getDateAndPlace().sellDate()).isEqualTo(editedDate.minusDays(1));
        assertThat(pdfReceipt.getGrossTotal()).isEqualByComparingTo("258.72");
        assertThat(pdfReceipt.getItems().get(0).getVatAmount()).isEqualByComparingTo("46.12");
        assertThat(pdfReceipt.getItems().get(1).getVatRate()).isEqualTo(VatRate.VAT_0);
    }

    @Test
    void saveUpdatesExistingItemsAndDatesButPreservesIdentityAndPhoneDetails() {
        LocalDateTime created = entity.getCreateDate();
        service.editPdf(entity.getTechnicalId(), request(), true);
        verify(repository).save(entity);
        assertThat(entity.getNumber()).isEqualTo("001/10/2026");
        assertThat(entity.getCreateDate()).isEqualTo(created);
        assertThat(entity.getCreatedBy()).isEqualTo("seller");
        assertThat(entity.getCustomerNote()).isEqualTo("Uwagi klienta");
        assertThat(entity.getDateAndPlace().getGenerateDate()).isEqualTo(editedDate);
        assertThat(entity.getDateAndPlace().getSellDate()).isEqualTo(editedDate.minusDays(1));
        assertThat(entity.getItems().get(0)).isSameAs(phone);
        assertThat(phone.getId()).isEqualTo(10L);
        assertThat(phone.getNettAmount()).isEqualByComparingTo("200.50");
        assertThat(phone.getGrossAmount()).isEqualByComparingTo("246.62");
        assertThat(phone.getWarrantyMonths()).isEqualTo(12);
        assertThat(phone.getUsed()).isTrue();
        assertThat(phone.getSellingInfo()).isEqualTo("Notatka");
        assertThat(accessory.getGrossAmount()).isEqualByComparingTo("12.10");
    }

    @Test
    void failedPdfGenerationDoesNotApplyChanges() {
        when(pdfService.generateReceiptPdf(any())).thenThrow(new IllegalStateException("PDF error"));
        assertThatThrownBy(() -> service.editPdf(entity.getTechnicalId(), request(), true))
            .isInstanceOf(IllegalStateException.class);
        assertThat(entity.getDateAndPlace().getGenerateDate()).isEqualTo(originalDate);
        assertThat(phone.getNettAmount()).isEqualByComparingTo("100.00");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsForeignMissingAndDuplicateItems() {
        for (List<ReceiptEditRequest.EditedItem> items : List.of(
            List.of(new ReceiptEditRequest.EditedItem(99L, BigDecimal.ONE, "ZW"), request().items().get(1)),
            List.of(request().items().get(0)),
            List.of(request().items().get(0), request().items().get(0)))) {
            assertThatThrownBy(() -> service.editPdf(entity.getTechnicalId(),
                new ReceiptEditRequest(editedDate, editedDate, items), true))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value()).isEqualTo(400);
        }
        verifyNoInteractions(pdfService);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsWithdrawnAndMissingReceipts() {
        entity.setStatus(ReceiptStatus.WYCOFANA);
        assertThatThrownBy(() -> service.editPdf(entity.getTechnicalId(), request(), true))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409);
        assertThatThrownBy(() -> service.getForEditing(UUID.randomUUID()))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404);
        verifyNoInteractions(pdfService);
    }
}
