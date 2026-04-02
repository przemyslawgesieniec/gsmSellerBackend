package pl.gesieniec.gsmseller.offer.model;

import lombok.Builder;
import pl.gesieniec.gsmseller.offer.model.specs.CommunicationSpecs;
import pl.gesieniec.gsmseller.offer.model.specs.ScreenSpecs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record PhoneOffer(
    UUID technicalId,
    BigDecimal price,
    String brand,
    String model,
    String status,
    String color,
    String location,
    ScreenSpecs screen,
    String memory,
    String ram,
    String simCardType,
    List<Integer> frontCamerasMpx,
    List<Integer> backCamerasMpx,
    String batteryCapacity,
    CommunicationSpecs communication,
    String operatingSystem,
    List<UUID> photos,
    Boolean isReserved
) {}
