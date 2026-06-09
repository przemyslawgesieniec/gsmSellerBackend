package pl.gesieniec.gsmseller.repair;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.gesieniec.gsmseller.repair.model.RepairDto;
import pl.gesieniec.gsmseller.repair.model.RepairNoteDto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RepairMapper {
    @Mapping(target = "clientTechnicalId", source = "client.technicalId")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "clientSurname", source = "client.surname")
    @Mapping(target = "clientPhoneNumber", source = "client.phoneNumber")
    @Mapping(target = "servicePointTechnicalId", source = "servicePoint.technicalId")
    @Mapping(target = "servicePointName", source = "servicePoint.name")
    RepairDto toDto(Repair repair);

    default List<RepairNoteDto> mapNotes(List<RepairNote> notes) {
        if (notes == null) return null;
        return notes.stream()
                .map(this::toNoteDto)
                .toList();
    }

    default RepairNoteDto toNoteDto(RepairNote note) {
        if (note == null) return null;
        return RepairNoteDto.builder()
                .id(note.getId())
                .content(note.getContent())
                .authorName(note.getAuthor().getUsername())
                .createdAt(note.getCreatedAt())
                .build();
    }

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "servicePoint", ignore = true)
    Repair toEntity(RepairDto dto);
}
