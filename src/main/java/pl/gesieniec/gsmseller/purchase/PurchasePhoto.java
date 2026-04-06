package pl.gesieniec.gsmseller.purchase;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "purchase_photos")
public class PurchasePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID technicalId;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private String contentType;

    public PurchasePhoto(byte[] data, String contentType) {
        this.technicalId = UUID.randomUUID();
        this.data = data;
        this.contentType = contentType;
    }
}
