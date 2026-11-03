package pl.gesieniec.gsmseller.eventlog;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
class EventLogTypeResolver {

    EventLogType resolve(String method, String path) {
        if ("/api/v1/auth/register".equals(path)) return EventLogType.USER_REGISTERED;
        if ("/api/v1/auth/reset-password".equals(path)) return EventLogType.USER_PASSWORD_RESET;

        if (path.startsWith("/api/v1/phones/")) {
            if (path.endsWith("/model")) return EventLogType.PHONE_MODEL_ASSIGNED;
            if (path.endsWith("/accept")) return EventLogType.PHONE_ACCEPTED_AT_LOCATION;
            if (path.endsWith("/restore")) return EventLogType.PHONE_RESTORED;
            if (path.endsWith("/remove-from-location")) return EventLogType.PHONE_REMOVED_FROM_LOCATION;
            if (path.endsWith("/handover")) return EventLogType.PHONE_HANDED_OVER;
            if (HttpMethod.DELETE.matches(method)) return EventLogType.PHONE_REMOVED;
            if (HttpMethod.PATCH.matches(method)) return EventLogType.PHONE_UPDATED;
        }
        if ("/api/v1/phones".equals(path) && HttpMethod.POST.matches(method)) return EventLogType.PHONE_CREATED;

        if (path.startsWith("/api/v1/offers")) {
            if (HttpMethod.POST.matches(method)) return EventLogType.OFFER_CREATED;
            if (HttpMethod.PUT.matches(method)) return EventLogType.OFFER_UPDATED;
            if (HttpMethod.DELETE.matches(method)) return EventLogType.OFFER_REMOVED;
        }
        if ("/api/v1/external/purchase".equals(path) && HttpMethod.POST.matches(method)) return EventLogType.PURCHASE_CREATED;
        if (path.startsWith("/api/v1/purchase/")) {
            if (path.endsWith("/close")) return EventLogType.PURCHASE_CLOSED;
            if (path.endsWith("/price-agreed")) return EventLogType.PURCHASE_PRICE_AGREED;
            if (path.endsWith("/success")) return EventLogType.PURCHASE_COMPLETED;
            if (path.endsWith("/comments")) return EventLogType.PURCHASE_COMMENT_ADDED;
        }
        if (path.startsWith("/api/v1/cart/")) {
            if (HttpMethod.DELETE.matches(method)) return EventLogType.CART_ITEM_REMOVED;
            if (HttpMethod.PATCH.matches(method)) return EventLogType.CART_ITEM_UPDATED;
            return EventLogType.CART_ITEM_ADDED;
        }
        if (path.startsWith("/api/v1/receipt/")) {
            if (path.endsWith("/cancel")) return EventLogType.RECEIPT_CANCELLED;
            if (path.endsWith("/edit")) return EventLogType.RECEIPT_EDITED;
        }
        if ("/api/v1/receipt".equals(path) && HttpMethod.POST.matches(method)) return EventLogType.RECEIPT_CREATED;

        if (path.startsWith("/api/v1/repairs/")) {
            if (path.contains("/notes/")) return EventLogType.REPAIR_NOTE_REMOVED;
            if (path.endsWith("/notes")) return EventLogType.REPAIR_NOTE_ADDED;
            if (path.endsWith("/status")) return EventLogType.REPAIR_STATUS_CHANGED;
            if (path.endsWith("/restore-to-shop")) return EventLogType.REPAIR_RESTORED_TO_SHOP;
            return EventLogType.REPAIR_UPDATED;
        }
        if ("/api/v1/repairs".equals(path)) return EventLogType.REPAIR_CREATED;
        if ("/api/v1/repair-service-points".equals(path) && HttpMethod.POST.matches(method)) {
            return EventLogType.REPAIR_SERVICE_POINT_CREATED;
        }
        if (path.startsWith("/api/v1/repair-clients")) {
            if (HttpMethod.POST.matches(method)) return EventLogType.REPAIR_CLIENT_CREATED;
            if (HttpMethod.PUT.matches(method)) return EventLogType.REPAIR_CLIENT_UPDATED;
            if (HttpMethod.DELETE.matches(method)) return EventLogType.REPAIR_CLIENT_REMOVED;
        }
        if (path.startsWith("/api/v1/reservations") || path.startsWith("/api/v1/external/reservations")) {
            return HttpMethod.DELETE.matches(method) ? EventLogType.RESERVATION_CANCELLED : EventLogType.RESERVATION_CREATED;
        }
        if (path.startsWith("/api/v1/locations")) {
            return HttpMethod.POST.matches(method) ? EventLogType.LOCATION_CREATED : EventLogType.LOCATION_UPDATED;
        }
        if (path.startsWith("/api/v1/phone-models")) {
            if (HttpMethod.POST.matches(method)) return EventLogType.PHONE_MODEL_CREATED;
            if (HttpMethod.PUT.matches(method)) return EventLogType.PHONE_MODEL_UPDATED;
            if (HttpMethod.DELETE.matches(method)) return EventLogType.PHONE_MODEL_REMOVED;
        }
        if (path.startsWith("/api/v1/admin/users/")) {
            return path.endsWith("/dynamic-location") ? EventLogType.USER_DYNAMIC_LOCATION_CHANGED : EventLogType.USER_ACTIVATION_CHANGED;
        }
        if (path.startsWith("/api/v1/users/location")) return EventLogType.USER_LOCATION_CHANGED;
        if (path.startsWith("/api/v1/notifications")) return EventLogType.NOTIFICATIONS_MARKED_READ;
        return EventLogType.SYSTEM_MUTATION;
    }
}
