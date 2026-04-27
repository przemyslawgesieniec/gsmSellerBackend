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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    private String frontCameras;
    private String backCameras;

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
    private Boolean isReserved = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private List<OfferPhoto> photos = new ArrayList<>();

    @Builder
    public Offer(PhoneStock phoneStock, ScreenSpecs screen, String memory, String ram, String simCardType,
                 List<Integer> frontCamerasMpx, List<Integer> backCamerasMpx,
                 String batteryCapacity, CommunicationSpecs communication, String operatingSystem,
                 String brand, List<OfferPhoto> photos, Boolean isReserved) {
        this.phoneStock = phoneStock;
        this.screen = screen;
        this.memory = memory;
        this.ram = ram;
        this.simCardType = simCardType;
        this.frontCameras = mapCamerasToString(frontCamerasMpx);
        this.backCameras = mapCamerasToString(backCamerasMpx);
        this.batteryCapacity = batteryCapacity;
        this.communication = communication;
        this.operatingSystem = operatingSystem;
        this.brand = brand;
        this.isReserved = isReserved;
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
        this.frontCameras = mapCamerasToString(frontCamerasMpx);
        this.backCameras = mapCamerasToString(backCamerasMpx);
        this.batteryCapacity = batteryCapacity;
        this.communication = communication;
        this.operatingSystem = operatingSystem;
        this.brand = brand;
        setPhotos(photos);
    }

    public List<Integer> getFrontCamerasMpx() {
        return mapCamerasToList(this.frontCameras);
    }

    public List<Integer> getBackCamerasMpx() {
        return mapCamerasToList(this.backCameras);
    }

    private String mapCamerasToString(List<Integer> cameras) {
        if (cameras == null || cameras.isEmpty()) {
            return "";
        }
        return cameras.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    private List<Integer> mapCamerasToList(String cameras) {
        if (cameras == null || cameras.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(cameras.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Integer::parseInt)
            .collect(Collectors.toList());
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

    public void setReserved(Boolean reserved) {
        this.isReserved = reserved;
    }

    public boolean isReserved() {
        return Boolean.TRUE.equals(isReserved);
    }
}
