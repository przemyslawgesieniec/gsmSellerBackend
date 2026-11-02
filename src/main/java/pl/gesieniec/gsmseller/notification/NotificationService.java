package pl.gesieniec.gsmseller.notification;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.reservation.Reservation;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserRepository;
import pl.gesieniec.gsmseller.user.UserStatus;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("dd.MM, HH:mm");

    private final AppNotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final PhoneStockRepository phoneStockRepository;

    @Transactional
    public void createReservationNotification(Reservation reservation) {
        String phoneName = phoneStockRepository.findByTechnicalId(reservation.getTechnicalId())
            .map(phone -> phone.getName() == null || phone.getName().isBlank() ? phone.getModel() : phone.getName())
            .orElse("telefon");
        String expiry = reservation.getExpiryTime().format(EXPIRY_FORMAT);
        AppNotification notification = notificationRepository.save(new AppNotification(
            NotificationType.RESERVATION_CREATED,
            "Nowa rezerwacja telefonu",
            "%s zarezerwował(a) %s. Rezerwacja wygasa %s.".formatted(reservation.getName(), phoneName, expiry),
            reservation.getTechnicalId()
        ));

        List<User> activeEmployees = userRepository.findByStatus(UserStatus.ACTIVE);
        recipientRepository.saveAll(activeEmployees.stream()
            .map(user -> new NotificationRecipient(notification, user))
            .toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listForUser(Long userId, boolean unreadOnly) {
        List<NotificationRecipient> recipients = unreadOnly
            ? recipientRepository.findTop100ByUserIdAndReadAtIsNullOrderByNotificationCreatedAtDesc(userId)
            : recipientRepository.findTop100ByUserIdOrderByNotificationCreatedAtDesc(userId);
        return recipients.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return recipientRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long recipientId, Long userId) {
        recipientRepository.findByIdAndUserId(recipientId, userId)
            .ifPresent(NotificationRecipient::markRead);
    }

    @Transactional
    public void markAllRead(Long userId) {
        recipientRepository.findByUserIdAndReadAtIsNull(userId)
            .forEach(NotificationRecipient::markRead);
    }

    private NotificationDto toDto(NotificationRecipient recipient) {
        AppNotification notification = recipient.getNotification();
        return new NotificationDto(
            recipient.getId(), notification.getType(), notification.getTitle(), notification.getContent(),
            notification.getCreatedAt(), recipient.getReadAt(), notification.getReservationTechnicalId()
        );
    }
}
