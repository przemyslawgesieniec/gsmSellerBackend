package pl.gesieniec.gsmseller.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    List<NotificationRecipient> findTop100ByUserIdOrderByNotificationCreatedAtDesc(Long userId);

    List<NotificationRecipient> findTop100ByUserIdAndReadAtIsNullOrderByNotificationCreatedAtDesc(Long userId);

    List<NotificationRecipient> findByUserIdAndReadAtIsNull(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<NotificationRecipient> findByIdAndUserId(Long id, Long userId);
}
