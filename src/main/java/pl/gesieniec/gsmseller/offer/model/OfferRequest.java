package pl.gesieniec.gsmseller.offer.model;

import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record OfferRequest(
    UUID phoneStockTechnicalId,
    String screenSize,
    String batteryCapacity,
    String screenType,
    List<String> photos
) {}
