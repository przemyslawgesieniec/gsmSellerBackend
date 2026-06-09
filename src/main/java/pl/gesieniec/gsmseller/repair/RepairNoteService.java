package pl.gesieniec.gsmseller.repair;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.repair.model.RepairNoteDto;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepairNoteService {

    private final RepairNoteRepository repairNoteRepository;
    private final RepairRepository repairRepository;
    private final UserService userService;
    private final RepairMapper repairMapper;

    @Transactional
    public RepairNoteDto addNote(UUID repairTechnicalId, String content, String username) {
        Repair repair = repairRepository.findByTechnicalId(repairTechnicalId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono naprawy o ID: " + repairTechnicalId));

        User author = userService.getUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika: " + username));

        RepairNote note = new RepairNote(repair, content, author);
        RepairNote savedNote = repairNoteRepository.save(note);

        return repairMapper.toNoteDto(savedNote);
    }

    @Transactional
    public void deleteNote(Long noteId, String username) {
        RepairNote note = repairNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono notatki o ID: " + noteId));

        if (!note.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("Tylko autor może usunąć swoją notatkę");
        }

        repairNoteRepository.delete(note);
    }
}
