package pl.gesieniec.gsmseller.eventlog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;

/** Publishes one audit event for each API operation that can modify application state. */
@Component
@RequiredArgsConstructor
public class EventLogInterceptor implements HandlerInterceptor {

    private static final Pattern IMEI_IN_BODY = Pattern.compile("\\\"imei\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern UUID_IN_PATH = Pattern.compile("/api/v1/phones/([0-9a-fA-F-]{36})(?:/|$)");

    private final EventLogPublisher eventLogPublisher;
    private final EventLogTypeResolver eventLogTypeResolver;
    private final PhoneStockRepository phoneStockRepository;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!isStateChanging(request.getMethod())) {
            return;
        }

        String path = request.getRequestURI();
        EventLogOutcome outcome = ex == null && response.getStatus() < 400 ? EventLogOutcome.SUCCESS : EventLogOutcome.FAILURE;
        eventLogPublisher.publish(
            eventLogTypeResolver.resolve(request.getMethod(), path),
            outcome,
            currentUsername(request),
            resolveImei(request, path),
            request.getMethod(),
            path,
            "HTTP " + response.getStatus()
        );
    }

    private boolean isStateChanging(String method) {
        return HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method)
            || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method);
    }

    private String currentUsername(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return Optional.ofNullable(request.getParameter("email"))
            .or(() -> Optional.ofNullable(request.getParameter("username")))
            .orElse(null);
    }

    private String resolveImei(HttpServletRequest request, String path) {
        String fromParameter = request.getParameter("imei");
        if (fromParameter != null && !fromParameter.isBlank()) return fromParameter;

        Matcher phoneId = UUID_IN_PATH.matcher(path);
        if (phoneId.find()) {
            try {
                return phoneStockRepository.findByTechnicalId(UUID.fromString(phoneId.group(1)))
                    .map(phone -> phone.getImei()).orElse(null);
            } catch (IllegalArgumentException ignored) {
                // A malformed path is still auditable, but has no IMEI.
            }
        }

        if (request instanceof ContentCachingRequestWrapper cachedRequest) {
            Matcher imei = IMEI_IN_BODY.matcher(new String(cachedRequest.getContentAsByteArray(), StandardCharsets.UTF_8));
            return imei.find() ? imei.group(1) : null;
        }
        return null;
    }
}
