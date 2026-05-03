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

    @Lob
    @Column
    private byte[] data;

    @Lob
    @Column
    @Setter
    private byte[] thumbnailData;

    @Column(nullable = false)
    private String contentType;

    @Column
    @Setter
    private String imageId;

    public OfferPhoto(byte[] data, byte[] thumbnailData, String contentType, String imageId) {
        this.technicalId = UUID.randomUUID();
        this.data = data;
        this.thumbnailData = thumbnailData;
        this.contentType = contentType;
        this.imageId = imageId;
    }
}
