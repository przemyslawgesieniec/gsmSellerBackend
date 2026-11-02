package pl.gesieniec.gsmseller.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.user.User;

/** The read timestamp belongs to a recipient, never to the shared notification. */
@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "notification_recipient",
    uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "user_id"})
)
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private AppNotification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private ZonedDateTime readAt;

    public NotificationRecipient(AppNotification notification, User user) {
        this.notification = notification;
        this.user = user;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = ZonedDateTime.now();
        }
    }
}
