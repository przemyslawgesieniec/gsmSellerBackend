package pl.gesieniec.gsmseller.phone.model;

import java.util.UUID;

public record PhoneModelFilterOption(
    UUID technicalId,
    UUID brandTechnicalId,
    String brand,
    String model,
    String displayName,
    String memory,
    String ram,
    String colors,
    String simCardType
) {}
