package pl.gesieniec.gsmseller.offer.model;

import lombok.Builder;
import java.util.UUID;

@Builder
public record Photo(
    UUID uuid,
    String thumbnailUrl,
    String galleryUrl,
    String publicUrl
) {}
