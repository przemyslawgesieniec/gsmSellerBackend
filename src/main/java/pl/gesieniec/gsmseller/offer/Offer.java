package pl.gesieniec.gsmseller.offer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.offer.model.specs.CommunicationSpecs;
import pl.gesieniec.gsmseller.offer.model.specs.ScreenSpecs;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "size", column = @Column(name = "screen_size")),
        @AttributeOverride(name = "resolution", column = @Column(name = "screen_resolution")),
        @AttributeOverride(name = "type", column = @Column(name = "screen_type"))
    })
    private ScreenSpecs screen;

    private String memory;
    private String ram;
    private String simCardType;

    @ElementCollection
    @CollectionTable(name = "offer_front_cameras", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "mpx")
    private List<Integer> frontCamerasMpx = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "offer_back_cameras", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "mpx")
    private List<Integer> backCamerasMpx = new ArrayList<>();

    private String batteryCapacity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "wifi", column = @Column(name = "comm_wifi")),
        @AttributeOverride(name = "portType", column = @Column(name = "comm_port_type")),
        @AttributeOverride(name = "bluetoothVersion", column = @Column(name = "comm_bluetooth_version"))
    })
    private CommunicationSpecs communication;

    private String operatingSystem;
    private String brand;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private List<OfferPhoto> photos = new ArrayList<>();

    @Builder
    public Offer(PhoneStock phoneStock, ScreenSpecs screen, String memory, String ram, String simCardType,
                 List<Integer> frontCamerasMpx, List<Integer> backCamerasMpx,
                 String batteryCapacity, CommunicationSpecs communication, String operatingSystem,
                 String brand, List<OfferPhoto> photos) {
        this.phoneStock = phoneStock;
        this.screen = screen;
        this.memory = memory;
        this.ram = ram;
        this.simCardType = simCardType;
        this.frontCamerasMpx = frontCamerasMpx != null ? new ArrayList<>(frontCamerasMpx) : new ArrayList<>();
        this.backCamerasMpx = backCamerasMpx != null ? new ArrayList<>(backCamerasMpx) : new ArrayList<>();
        this.batteryCapacity = batteryCapacity;
        this.communication = communication;
        this.operatingSystem = operatingSystem;
        this.brand = brand;
        setPhotos(photos);
    }

    public void update(ScreenSpecs screen, String memory, String ram, String simCardType,
                       List<Integer> frontCamerasMpx, List<Integer> backCamerasMpx,
                       String batteryCapacity, CommunicationSpecs communication, String operatingSystem,
                       String brand, List<OfferPhoto> photos) {
        this.screen = screen;
        this.memory = memory;
        this.ram = ram;
        this.simCardType = simCardType;
        this.frontCamerasMpx = frontCamerasMpx != null ? new ArrayList<>(frontCamerasMpx) : new ArrayList<>();
        this.backCamerasMpx = backCamerasMpx != null ? new ArrayList<>(backCamerasMpx) : new ArrayList<>();
        this.batteryCapacity = batteryCapacity;
        this.communication = communication;
        this.operatingSystem = operatingSystem;
        this.brand = brand;
        setPhotos(photos);
    }

    public void setPhotos(List<OfferPhoto> photos) {
        if (photos != null && photos.size() > 5) {
            throw new IllegalArgumentException("Offer cannot have more than 5 photos");
        }
        this.photos.clear();
        if (photos != null) {
            this.photos.addAll(photos);
        }
    }
}
