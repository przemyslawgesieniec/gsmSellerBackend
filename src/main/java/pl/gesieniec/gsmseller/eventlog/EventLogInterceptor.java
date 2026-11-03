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
import org.springframework.web.util.ContentCachingResponseWrapper;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;

/** Publishes one audit event for each API operation that can modify application state. */
@Component
@RequiredArgsConstructor
public class EventLogInterceptor implements HandlerInterceptor {

    private static final Pattern IMEI_IN_BODY = Pattern.compile("\\\"imei\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOURCE_UUID_IN_PATH = Pattern.compile("/api/v1/(?:phones|repairs|receipt|offers)/(?:[a-z-]+/)?([0-9a-fA-F-]{36})(?:/|$)");
    private static final Pattern CART_UUID_IN_PATH = Pattern.compile("/api/v1/cart/(?:item/)?([0-9a-fA-F-]{36})(?:/|$)");
    private static final Pattern UUID_IN_RESPONSE = Pattern.compile("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}");

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
        UUID resourceTechnicalId = resolveResourceTechnicalId(request, response, path);
        eventLogPublisher.publish(
            eventLogTypeResolver.resolve(request.getMethod(), path),
            outcome,
            currentUsername(request),
            resolveImei(request, path, resourceTechnicalId),
            request.getMethod(),
            path,
            "HTTP " + response.getStatus(),
            resourceTechnicalId
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

    private String resolveImei(HttpServletRequest request, String path, UUID resourceTechnicalId) {
        String fromParameter = request.getParameter("imei");
        if (fromParameter != null && !fromParameter.isBlank()) return fromParameter;

        if (resourceTechnicalId != null) {
            return phoneStockRepository.findByTechnicalId(resourceTechnicalId)
                .map(phone -> phone.getImei()).orElseGet(() -> imeiFromCachedRequest(request));
        }

        return imeiFromCachedRequest(request);
    }

    private String imeiFromCachedRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper cachedRequest) {
            Matcher imei = IMEI_IN_BODY.matcher(new String(cachedRequest.getContentAsByteArray(), StandardCharsets.UTF_8));
            return imei.find() ? imei.group(1) : null;
        }
        return null;
    }

    private UUID resolveResourceTechnicalId(HttpServletRequest request, HttpServletResponse response, String path) {
        String requestTechnicalId = request.getParameter("technicalId");
        UUID fromRequest = parseUuid(requestTechnicalId);
        if (fromRequest != null) return fromRequest;

        UUID fromPath = findUuid(RESOURCE_UUID_IN_PATH, path);
        if (fromPath != null) return fromPath;
        UUID fromCartPath = findUuid(CART_UUID_IN_PATH, path);
        if (fromCartPath != null) return fromCartPath;

        if ("POST".equals(request.getMethod()) && ("/api/v1/receipt".equals(path) || "/api/v1/repairs".equals(path))
            && response instanceof ContentCachingResponseWrapper cachedResponse) {
            Matcher responseUuid = UUID_IN_RESPONSE.matcher(new String(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
            if (responseUuid.find()) return parseUuid(responseUuid.group());
        }
        return null;
    }

    private UUID findUuid(Pattern pattern, String path) {
        Matcher matcher = pattern.matcher(path);
        return matcher.find() ? parseUuid(matcher.group(1)) : null;
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
