package pl.gesieniec.gsmseller.offer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfferPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID technicalId;

    @Column(nullable = false)
    private String contentType;

    @Column
    @Setter
    private String imageId;

    @Column(name = "display_order")
    @Setter
    private Integer displayOrder;

    public OfferPhoto(String contentType, String imageId) {
        this.technicalId = UUID.randomUUID();
        this.contentType = contentType;
        this.imageId = imageId;
    }
}
