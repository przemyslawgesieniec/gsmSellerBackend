package pl.gesieniec.gsmseller.repair;

import org.mapstruct.Mapper;
import pl.gesieniec.gsmseller.repair.model.RepairDto;

@Mapper(componentModel = "spring")
public interface RepairMapper {
    RepairDto toDto(Repair repair);
    Repair toEntity(RepairDto dto);
}
