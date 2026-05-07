package pl.gesieniec.gsmseller.phone.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "phone_models")
public class PhoneModels {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID technicalId;

    @Column(nullable = false)
    private String model;

    private String screen;
    private String screenResolution;
    private String displayType;
    private String memory;
    private String ram;
    private String simCardType;
    private String portType;
    private Boolean dualSim = false;
    private String colors;
    private String frontCameras;
    private String backCameras;
    private String batteryCapacity;
    private String brand;
    @Column(columnDefinition = "integer default 0")
    private Integer displayPriority = 0;

    public PhoneModels(String model, String screen, String screenResolution, String displayType, String memory, String ram,
                       String simCardType, String portType, Boolean dualSim, String colors, String frontCameras, String backCameras,
                       String batteryCapacity, String brand, Integer displayPriority) {
        this.technicalId = UUID.randomUUID();
        this.model = model;
        this.screen = screen;
        this.screenResolution = screenResolution;
        this.displayType = displayType;
        this.memory = memory;
        this.ram = ram;
        this.simCardType = simCardType;
        this.portType = portType;
        this.dualSim = dualSim;
        this.colors = colors;
        this.frontCameras = frontCameras;
        this.backCameras = backCameras;
        this.batteryCapacity = batteryCapacity;
        this.brand = brand;
        this.displayPriority = normalizeDisplayPriority(displayPriority);
    }

    public void update(String model, String screen, String screenResolution, String displayType, String memory, String ram,
                       String simCardType, String portType, Boolean dualSim, String colors, String frontCameras, String backCameras,
                       String batteryCapacity, String brand, Integer displayPriority) {
        this.model = model;
        this.screen = screen;
        this.screenResolution = screenResolution;
        this.displayType = displayType;
        this.memory = memory;
        this.ram = ram;
        this.simCardType = simCardType;
        this.portType = portType;
        this.dualSim = dualSim;
        this.colors = colors;
        this.frontCameras = frontCameras;
        this.backCameras = backCameras;
        this.batteryCapacity = batteryCapacity;
        this.brand = brand;
        this.displayPriority = normalizeDisplayPriority(displayPriority);
    }

    public String getDisplayName() {
        if (brand == null || brand.isBlank()) {
            return model;
        }
        return brand + " " + model;
    }

    public List<Integer> getFrontCamerasMpx() {
        return mapCamerasToList(frontCameras);
    }

    public List<Integer> getBackCamerasMpx() {
        return mapCamerasToList(backCameras);
    }

    public Integer getDisplayPriority() {
        return displayPriority == null ? 0 : displayPriority;
    }

    private Integer normalizeDisplayPriority(Integer displayPriority) {
        if (displayPriority == null) {
            return 0;
        }
        return Math.max(0, Math.min(10, displayPriority));
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
}
