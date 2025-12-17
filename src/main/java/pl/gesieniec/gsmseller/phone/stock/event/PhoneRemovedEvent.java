package pl.gesieniec.gsmseller.phone.stock.event;

import java.util.UUID;

public record PhoneRemovedEvent(UUID phoneTechnicalId) {
}
