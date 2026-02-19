package pl.gesieniec.gsmseller.repair;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.gesieniec.gsmseller.repair.model.RepairDto;

@Mapper(componentModel = "spring")
public interface RepairMapper {
    @Mapping(target = "clientTechnicalId", source = "client.technicalId")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "clientSurname", source = "client.surname")
    @Mapping(target = "clientPhoneNumber", source = "client.phoneNumber")
    RepairDto toDto(Repair repair);

    @Mapping(target = "client", ignore = true)
    Repair toEntity(RepairDto dto);
}
