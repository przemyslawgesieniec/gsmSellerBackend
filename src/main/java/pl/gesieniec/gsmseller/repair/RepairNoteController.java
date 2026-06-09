package pl.gesieniec.gsmseller.repair;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pl.gesieniec.gsmseller.repair.model.RepairNoteDto;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/repairs/{technicalId}/notes")
@RequiredArgsConstructor
public class RepairNoteController {

    private final RepairNoteService service;

    @PostMapping
    public RepairNoteDto addNote(
            @PathVariable UUID technicalId,
            @RequestBody String content,
            Principal principal
    ) {
        log.info("Dodawanie notatki do naprawy {}: {}", technicalId, content);
        return service.addNote(technicalId, content, principal.getName());
    }

    @DeleteMapping("/{noteId}")
    public void deleteNote(
            @PathVariable UUID technicalId,
            @PathVariable Long noteId,
            Principal principal
    ) {
        log.info("Usuwanie notatki {} z naprawy {}", noteId, technicalId);
        service.deleteNote(noteId, principal.getName());
    }
}
