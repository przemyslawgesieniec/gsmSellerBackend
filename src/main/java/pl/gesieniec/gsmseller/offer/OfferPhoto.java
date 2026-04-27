package pl.gesieniec.gsmseller.offer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(nullable = false)
    private byte[] data;

    @Lob
    @Column
    private byte[] thumbnailData;

    @Column(nullable = false)
    private String contentType;

    public OfferPhoto(byte[] data, byte[] thumbnailData, String contentType) {
        this.technicalId = UUID.randomUUID();
        this.data = data;
        this.thumbnailData = thumbnailData;
        this.contentType = contentType;
    }
}
