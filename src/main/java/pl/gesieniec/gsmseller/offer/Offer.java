package pl.gesieniec.gsmseller.offer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_stock_id", unique = true)
    private PhoneStock phoneStock;

    private String screenSize;
    private String batteryCapacity;
    private String screenType;

    @ElementCollection
    @CollectionTable(name = "offer_photos", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "photo_url")
    private List<String> photos = new ArrayList<>();

    public Offer(PhoneStock phoneStock, String screenSize, String batteryCapacity, String screenType, List<String> photos) {
        this.phoneStock = phoneStock;
        this.screenSize = screenSize;
        this.batteryCapacity = batteryCapacity;
        this.screenType = screenType;
        setPhotos(photos);
    }

    public void update(String screenSize, String batteryCapacity, String screenType, List<String> photos) {
        this.screenSize = screenSize;
        this.batteryCapacity = batteryCapacity;
        this.screenType = screenType;
        setPhotos(photos);
    }

    public void setPhotos(List<String> photos) {
        if (photos != null && photos.size() > 5) {
            throw new IllegalArgumentException("Offer cannot have more than 5 photos");
        }
        this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
    }
}
