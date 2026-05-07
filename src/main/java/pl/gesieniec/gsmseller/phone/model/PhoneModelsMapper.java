package pl.gesieniec.gsmseller.phone.model;

import org.springframework.stereotype.Component;

@Component
public class PhoneModelsMapper {

    public PhoneModelsDto toDto(PhoneModels model) {
        return new PhoneModelsDto(
            model.getTechnicalId(),
            model.getModel(),
            model.getScreen(),
            model.getScreenResolution(),
            model.getDisplayType(),
            model.getMemory(),
            model.getRam(),
            model.getSimCardType(),
            model.getPortType(),
            model.getDualSim(),
            model.getColors(),
            model.getFrontCameras(),
            model.getBackCameras(),
            model.getBatteryCapacity(),
            model.getBrand(),
            model.getDisplayName(),
            model.getDisplayPriority()
        );
    }
}
