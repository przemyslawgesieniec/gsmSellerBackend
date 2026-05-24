package pl.gesieniec.gsmseller.phone.model;

import java.util.UUID;

public record PhoneModelFilterOption(
    UUID technicalId,
    String brand,
    String model,
    String displayName
) {}
