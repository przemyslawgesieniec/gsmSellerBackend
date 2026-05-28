package pl.gesieniec.gsmseller.phone.model;

import java.util.UUID;

public record PhoneBrandFilterOption(
    UUID technicalId,
    String name
) {}
