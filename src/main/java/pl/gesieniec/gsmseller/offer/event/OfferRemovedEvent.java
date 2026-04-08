package pl.gesieniec.gsmseller.offer.event;

import java.util.UUID;

public record OfferRemovedEvent(UUID phoneStockTechnicalId) {
}
