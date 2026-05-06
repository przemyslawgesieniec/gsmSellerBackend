package pl.gesieniec.gsmseller.phone.model;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneModelsDto {
    private UUID technicalId;
    private String model;
    private String screen;
    private String screenResolution;
    private String displayType;
    private String memory;
    private String ram;
    private String simCardType;
    private String portType;
    private Boolean dualSim;
    private String colors;
    private String frontCameras;
    private String backCameras;
    private String batteryCapacity;
    private String brand;
    private String displayName;
}
