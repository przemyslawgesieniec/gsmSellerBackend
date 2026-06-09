package pl.gesieniec.gsmseller.repair.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairNoteDto {
    private Long id;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
}
