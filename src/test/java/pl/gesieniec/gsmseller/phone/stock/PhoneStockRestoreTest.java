package pl.gesieniec.gsmseller.phone.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.phone.stock.model.PurchaseType;
import pl.gesieniec.gsmseller.phone.stock.model.Status;

class PhoneStockRestoreTest {

    @Test
    void shouldRestoreHandedOverPhoneAndClearHandoverSettlement() {
        LocationEntity location = new LocationEntity("Sklep", "Warszawa", "123456789");
        PhoneStock phone = phoneWithStatus(Status.DOSTĘPNY, location);
        phone.handover("Przekazano", "1200");

        phone.restore();

        assertThat(phone.getStatus()).isEqualTo(Status.DOSTĘPNY);
        assertThat(phone.getSoldFor()).isNull();
        assertThat(phone.getSoldAt()).isNull();
        assertThat(phone.getLocation()).isSameAs(location);
        assertThat(phone.getSellingPrice()).isEqualByComparingTo("1200");
        assertThat(phone.getComment()).isEqualTo("Przekazano");
    }

    @Test
    void shouldRestoreHandedOverPhoneWithoutLocationAsAvailable() {
        PhoneStock phone = phoneWithStatus(Status.ODDANY, null);

        phone.restore();

        assertThat(phone.getStatus()).isEqualTo(Status.DOSTĘPNY);
    }

    @Test
    void shouldKeepDeletedPhoneRestorationDependentOnLocation() {
        PhoneStock withoutLocation = phoneWithStatus(Status.USUNIĘTY, null);
        PhoneStock withLocation = phoneWithStatus(Status.USUNIĘTY, new LocationEntity());

        withoutLocation.restore();
        withLocation.restore();

        assertThat(withoutLocation.getStatus()).isEqualTo(Status.WPROWADZONY);
        assertThat(withLocation.getStatus()).isEqualTo(Status.DOSTĘPNY);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"DOSTĘPNY", "SPRZEDANY", "WPROWADZONY", "SERWIS"})
    void shouldRejectRestorationFromOtherStatuses(Status status) {
        PhoneStock phone = phoneWithStatus(status, null);

        assertThatThrownBy(phone::restore).isInstanceOf(IllegalStateException.class);
        assertThat(phone.getStatus()).isEqualTo(status);
    }

    private PhoneStock phoneWithStatus(Status status, LocationEntity location) {
        return new PhoneStock(UUID.randomUUID(), "Model", "8", "128", "Black", "SIM",
            "123456789012345", "Telefon", "Dostawca", new BigDecimal("1000"),
            new BigDecimal("1500"), location, status, PurchaseType.CASH, null, null, null, false);
    }
}
