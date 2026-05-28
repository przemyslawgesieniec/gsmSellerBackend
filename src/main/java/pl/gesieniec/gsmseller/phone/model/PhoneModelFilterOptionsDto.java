package pl.gesieniec.gsmseller.phone.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PhoneModelFilterOptionsDto(
    List<PhoneBrandFilterOption> brands,
    Map<UUID, List<PhoneModelFilterOption>> modelsByBrand
) {}
