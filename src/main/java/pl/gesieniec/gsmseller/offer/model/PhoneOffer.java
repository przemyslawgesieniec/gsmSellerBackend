package pl.gesieniec.gsmseller.offer.model;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record PhoneOffer(
    UUID technicalId,
    BigDecimal price,
    String brand,
    String model,
    String screenSize,
    String batteryCapacity,
    String screenType,
    List<String> photos
) {}
