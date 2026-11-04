package pl.gesieniec.gsmseller.phone.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.phone.stock.model.PurchaseType;
import pl.gesieniec.gsmseller.phone.stock.model.Status;

class PhoneStockMoveTest {

    @Test
    void movesAvailablePhoneToAnotherLocationWithoutChangingItsStatus() {
        LocationEntity source = new LocationEntity("Stryków", "Stryków", "123");
        LocationEntity destination = new LocationEntity("Łódź", "Łódź", "456");
        PhoneStock phone = phone(Status.DOSTĘPNY, source);

        phone.moveToLocation(destination);

        assertThat(phone.getLocation()).isSameAs(destination);
        assertThat(phone.getStatus()).isEqualTo(Status.DOSTĘPNY);
    }

    @Test
    void rejectsMoveWhenPhoneIsNotAvailable() {
        PhoneStock phone = phone(Status.WPROWADZONY, new LocationEntity("Stryków", "Stryków", "123"));

        assertThatThrownBy(() -> phone.moveToLocation(new LocationEntity("Łódź", "Łódź", "456")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("dostępny");
    }

    private PhoneStock phone(Status status, LocationEntity location) {
        return new PhoneStock(
            java.util.UUID.randomUUID(), "Model", null, null, "Kolor", null, "IMEI",
            "Telefon", "Źródło", BigDecimal.TEN, BigDecimal.ONE, location, status,
            PurchaseType.CASH, null, null, null, false
        );
    }
}
