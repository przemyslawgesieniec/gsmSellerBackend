package pl.gesieniec.gsmseller.eventlog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;

/** Publishes one audit event for each API operation that can modify application state. */
@Component
@RequiredArgsConstructor
public class EventLogInterceptor implements HandlerInterceptor {

    private static final Pattern IMEI_IN_BODY = Pattern.compile("\\\"imei\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOURCE_UUID_IN_PATH = Pattern.compile("/api/v1/(?:phones|repairs|receipt|offers)/(?:[a-z-]+/)?([0-9a-fA-F-]{36})(?:/|$)");
    private static final Pattern CART_UUID_IN_PATH = Pattern.compile("/api/v1/cart/(?:item/)?([0-9a-fA-F-]{36})(?:/|$)");
    private static final Pattern UUID_IN_RESPONSE = Pattern.compile("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}");
    private static final String PHONE_SNAPSHOT_ATTRIBUTE = EventLogInterceptor.class.getName() + ".phoneSnapshot";

    private final EventLogPublisher eventLogPublisher;
    private final EventLogTypeResolver eventLogTypeResolver;
    private final PhoneStockRepository phoneStockRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPhoneUpdate(request.getMethod(), request.getRequestURI())) {
            UUID phoneTechnicalId = findUuid(RESOURCE_UUID_IN_PATH, request.getRequestURI());
            if (phoneTechnicalId != null) {
                phoneStockRepository.findByTechnicalId(phoneTechnicalId)
                    .map(PhoneSnapshot::from)
                    .ifPresent(snapshot -> request.setAttribute(PHONE_SNAPSHOT_ATTRIBUTE, snapshot));
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!isStateChanging(request.getMethod())) {
            return;
        }

        String path = request.getRequestURI();
        EventLogOutcome outcome = ex == null && response.getStatus() < 400 ? EventLogOutcome.SUCCESS : EventLogOutcome.FAILURE;
        UUID resourceTechnicalId = resolveResourceTechnicalId(request, response, path);
        EventLogType eventType = eventLogTypeResolver.resolve(request.getMethod(), path);
        eventLogPublisher.publish(
            eventType,
            outcome,
            currentUsername(request),
            resolveImei(request, path, resourceTechnicalId),
            request.getMethod(),
            path,
            detailsFor(eventType, outcome, request, resourceTechnicalId, response.getStatus()),
            resourceTechnicalId
        );
    }

    private boolean isStateChanging(String method) {
        return HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method)
            || HttpMethod.PATCH.matches(method) || HttpMethod.DELETE.matches(method);
    }

    private boolean isPhoneUpdate(String method, String path) {
        return HttpMethod.PATCH.matches(method) && RESOURCE_UUID_IN_PATH.matcher(path).matches()
            && path.startsWith("/api/v1/phones/");
    }

    private String detailsFor(
        EventLogType eventType,
        EventLogOutcome outcome,
        HttpServletRequest request,
        UUID resourceTechnicalId,
        int status
    ) {
        if (eventType != EventLogType.PHONE_UPDATED || outcome != EventLogOutcome.SUCCESS || resourceTechnicalId == null) {
            return "HTTP " + status;
        }

        Object attribute = request.getAttribute(PHONE_SNAPSHOT_ATTRIBUTE);
        if (!(attribute instanceof PhoneSnapshot before)) {
            return "Telefon został zaktualizowany";
        }
        return phoneStockRepository.findByTechnicalId(resourceTechnicalId)
            .map(before::describeChanges)
            .orElse("Telefon został zaktualizowany");
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

    private record PhoneSnapshot(
        String name,
        String model,
        String ram,
        String memory,
        String color,
        String simCardType,
        String imei,
        String source,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        String description,
        String comment,
        boolean used,
        String batteryCondition
    ) {
        static PhoneSnapshot from(PhoneStock phone) {
            return new PhoneSnapshot(
                phone.getName(), phone.getModel(), phone.getRam(), phone.getMemory(), phone.getColor(),
                phone.getSimCardType(), phone.getImei(), phone.getSource(), phone.getPurchasePrice(),
                phone.getSellingPrice(), phone.getDescription(), phone.getComment(), phone.isUsed(), phone.getBatteryCondition()
            );
        }

        String describeChanges(PhoneStock after) {
            List<String> changes = new ArrayList<>();
            addChange(changes, "nazwa", name, after.getName());
            addChange(changes, "model", model, after.getModel());
            addChange(changes, "RAM", ram, after.getRam());
            addChange(changes, "pamięć", memory, after.getMemory());
            addChange(changes, "kolor", color, after.getColor());
            addChange(changes, "SIM", simCardType, after.getSimCardType());
            addChange(changes, "IMEI", imei, after.getImei());
            addChange(changes, "źródło", source, after.getSource());
            addChange(changes, "cena zakupu", purchasePrice, after.getPurchasePrice());
            addChange(changes, "cena sprzedaży", sellingPrice, after.getSellingPrice());
            addChange(changes, "opis", description, after.getDescription());
            addChange(changes, "komentarz", comment, after.getComment());
            addChange(changes, "używany", used, after.isUsed());
            addChange(changes, "kondycja baterii", batteryCondition, after.getBatteryCondition());
            return changes.isEmpty() ? "Zapisano bez zmiany wartości" : "Zmieniono: " + String.join("; ", changes);
        }

        private static void addChange(List<String> changes, String label, Object before, Object after) {
            if (!Objects.equals(before, after)) {
                changes.add(label + ": " + display(before) + " → " + display(after));
            }
        }

        private static String display(Object value) {
            return value == null || String.valueOf(value).isBlank() ? "brak" : String.valueOf(value);
        }
    }
}
