# Event log

## Cel i retencja

Moduł `pl.gesieniec.gsmseller.eventlog` zapisuje zdarzenia audytowe jako osobną tabelę `event_log`.
Operacje zmieniające stan, obsłużone przez REST API, są domyślnie rejestrowane przez
`EventLogInterceptor`. Po zakończeniu żądania interceptor publikuje Spring event
`EventLogRecordedEvent`; `EventLogListener` zapisuje go w bazie. Dzięki temu kod domenowy nie jest
powiązany z tabelą audytową.

Wpis zawiera czas UTC, typ, wynik (`SUCCESS` albo `FAILURE`), login wykonawcy, opcjonalne IMEI,
identyfikator techniczny obiektu, metodę i ścieżkę HTTP oraz status odpowiedzi. Ogólny zapis HTTP pomija treść żądań i sekrety. IMEI jest odczytywany z parametru/danych telefonu albo odnajdywany
po identyfikatorze telefonu. Identyfikator służy portalowi do przejścia bezpośrednio do naprawy lub
dokumentu sprzedaży.

Przy `PHONE_UPDATED` szczegóły zawierają listę pól, których wartość faktycznie zmieniła się, w formacie
`pole: wartość poprzednia → wartość nowa`.

Usunięcie naprawy zapisuje w tej samej transakcji pojedynczy wpis `REPAIR_UPDATED` z metodą `DELETE`.
Pole `deletedRepairSnapshot` zawiera dane naprawy, w tym zapisany kod blokady urządzenia, notatki oraz
dane powiązanego klienta i punktu serwisowego. Widok audytu pokazuje ten wpis jako „Usunięto naprawę”.

`EventLogService` usuwa dane starsze niż **30 dni** codziennie o `00:10` (cron można zmienić przez
`event-log.retention-cron`). Retencja jest liczona względem chwili wykonania czyszczenia.

## Dostęp administratora

Tylko użytkownik z `ROLE_ADMIN` ma dostęp do:

- `GET /api/v1/admin/event-log` — stronicowane wyszukiwanie;
- `GET /api/v1/admin/event-log/types` — typy do zasilenia filtra UI.

Parametry wyszukiwania pierwszego endpointu:

| Parametr | Znaczenie |
| --- | --- |
| `eventType` | dokładny typ z poniższej listy |
| `imei` | fragment IMEI, bez rozróżniania wielkości liter |
| `actorUsername` | fragment loginu pracownika, bez rozróżniania wielkości liter |
| `from`, `to` | opcjonalny zakres czasu ISO-8601 |
| `page`, `size`, `sort` | standardowe parametry Spring Data; domyślnie najnowsze wpisy |

Przykład: `GET /api/v1/admin/event-log?eventType=PHONE_UPDATED&imei=12345&actorUsername=anna&page=1&size=50`

## Zbierane typy zdarzeń

| Obszar | Typy |
| --- | --- |
| Uwierzytelnianie | `AUTHENTICATION_SUCCEEDED`, `AUTHENTICATION_FAILED`, `LOGOUT` |
| Telefony | `PHONE_CREATED`, `PHONE_UPDATED`, `PHONE_MODEL_ASSIGNED`, `PHONE_ACCEPTED_AT_LOCATION`, `PHONE_REMOVED`, `PHONE_RESTORED`, `PHONE_REMOVED_FROM_LOCATION`, `PHONE_HANDED_OVER`, `PHONE_SOLD` |
| Oferty | `OFFER_CREATED`, `OFFER_UPDATED`, `OFFER_REMOVED` |
| Skup | `PURCHASE_CREATED`, `PURCHASE_CLOSED`, `PURCHASE_PRICE_AGREED`, `PURCHASE_COMPLETED`, `PURCHASE_COMMENT_ADDED` |
| Koszyk i paragony | `CART_ITEM_ADDED`, `CART_ITEM_UPDATED`, `CART_ITEM_REMOVED`, `RECEIPT_CREATED`, `RECEIPT_CANCELLED`, `RECEIPT_EDITED` |
| Serwis | `REPAIR_CREATED`, `REPAIR_UPDATED`, `REPAIR_STATUS_CHANGED`, `REPAIR_RESTORED_TO_SHOP`, `REPAIR_NOTE_ADDED`, `REPAIR_NOTE_REMOVED`, `REPAIR_CLIENT_CREATED`, `REPAIR_CLIENT_UPDATED`, `REPAIR_CLIENT_REMOVED`, `REPAIR_SERVICE_POINT_CREATED` |
| Rezerwacje | `RESERVATION_CREATED`, `RESERVATION_CANCELLED`, `RESERVATION_EXPIRED` |
| Konfiguracja katalogu | `LOCATION_CREATED`, `LOCATION_UPDATED`, `PHONE_MODEL_CREATED`, `PHONE_MODEL_UPDATED`, `PHONE_MODEL_REMOVED` |
| Użytkownicy | `USER_REGISTERED`, `USER_PASSWORD_RESET`, `USER_ACTIVATION_CHANGED`, `USER_DYNAMIC_LOCATION_CHANGED`, `USER_LOCATION_CHANGED` |
| Powiadomienia | `NOTIFICATIONS_MARKED_READ` |
| Zapis rozszerzający | `SYSTEM_MUTATION` — zabezpiecza nowe modyfikujące endpointy, zanim dostaną własny typ |

Każdy z powyższych typów może mieć wynik `FAILURE`, jeżeli żądanie zakończy się kodem HTTP 4xx/5xx
albo nieobsłużonym wyjątkiem. Zdarzenia logowania i wylogowania są przechwytywane jako Spring
Security events, ponieważ nie przechodzą przez kontrolery MVC. Sprzedaż telefonu i automatyczne
wygaśnięcie rezerwacji są także zapisywane z ich istniejących zdarzeń domenowych Spring; dlatego
obejmują również pracę cykliczną, która nie ma żądania HTTP. Dla `RESERVATION_EXPIRED` kolumna
szczegółów zawiera imię i nazwisko (pole `name` rezerwacji) oraz numer telefonu osoby rezerwującej.
