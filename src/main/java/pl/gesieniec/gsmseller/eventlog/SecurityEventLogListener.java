package pl.gesieniec.gsmseller.eventlog;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

/** Audits authentication, which does not pass through MVC controller handlers. */
@Component
@RequiredArgsConstructor
class SecurityEventLogListener {

    private final EventLogPublisher eventLogPublisher;

    @EventListener
    public void authenticationSucceeded(AuthenticationSuccessEvent event) {
        eventLogPublisher.publish(EventLogType.AUTHENTICATION_SUCCEEDED, EventLogOutcome.SUCCESS,
            event.getAuthentication().getName(), null, "POST", "/login", "Authentication succeeded");
    }

    @EventListener
    public void authenticationFailed(AbstractAuthenticationFailureEvent event) {
        eventLogPublisher.publish(EventLogType.AUTHENTICATION_FAILED, EventLogOutcome.FAILURE,
            event.getAuthentication().getName(), null, "POST", "/login", "Authentication failed");
    }

    @EventListener
    public void logoutSucceeded(LogoutSuccessEvent event) {
        eventLogPublisher.publish(EventLogType.LOGOUT, EventLogOutcome.SUCCESS,
            event.getAuthentication().getName(), null, "POST", "/logout", "Logout succeeded");
    }
}
