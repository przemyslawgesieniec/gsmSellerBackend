package pl.gesieniec.gsmseller.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "app_notification")
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    /** Optional context for linking future notification types to application records. */
    private UUID reservationTechnicalId;

    public AppNotification(NotificationType type, String title, String content, UUID reservationTechnicalId) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.reservationTechnicalId = reservationTechnicalId;
        this.createdAt = ZonedDateTime.now();
    }
}
