package pl.gesieniec.gsmseller.repair.client;

import org.mapstruct.Mapper;
import pl.gesieniec.gsmseller.repair.model.RepairClientDto;

@Mapper(componentModel = "spring")
public interface RepairClientMapper {
    RepairClientDto toDto(RepairClient repairClient);
    RepairClient toEntity(RepairClientDto dto);
}
