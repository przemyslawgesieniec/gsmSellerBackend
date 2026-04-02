package pl.gesieniec.gsmseller.offer.model;

import lombok.Builder;
import pl.gesieniec.gsmseller.offer.model.specs.CommunicationSpecs;
import pl.gesieniec.gsmseller.offer.model.specs.ScreenSpecs;

import java.util.List;
import java.util.UUID;

@Builder
public record OfferRequest(
    UUID phoneStockTechnicalId,
    ScreenSpecs screen,
    String memory,
    String ram,
    String simCardType,
    List<Integer> frontCamerasMpx,
    List<Integer> backCamerasMpx,
    String batteryCapacity,
    CommunicationSpecs communication,
    String operatingSystem,
    String brand,
    List<UUID> photos
) {}
