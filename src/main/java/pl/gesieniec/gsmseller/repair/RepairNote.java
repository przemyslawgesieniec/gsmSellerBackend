package pl.gesieniec.gsmseller.repair;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.gesieniec.gsmseller.user.User;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "repair_notes")
public class RepairNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_id", nullable = false)
    private Repair repair;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public RepairNote(Repair repair, String content, User author) {
        this.repair = repair;
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }
}
