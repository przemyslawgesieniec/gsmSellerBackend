# GSM Workspace — frontend v2

Nowy interfejs: `/v2/index.html`. Klasyczny frontend pozostaje pod dotychczasowymi adresami.
Przełączniki w obu nawigacjach zachowują bieżący ekran i query string (np. identyfikator faktury).
Nowy ekran logowania: `/v2/login.html`. Oba widoki korzystają z tej samej sesji Spring Security.

## Architektura

Samodzielny frontend wielostronicowy w `src/main/resources/static/v2/`, bez Materialize,
bez iframe, bez pobierania HTML ani skryptów starego interfejsu. Każda strona zawiera
nową nawigację, układ, semantyczne pola i własne komponenty interakcji. Logika dziedzinowa
kontrolerów została przeniesiona z zachowaniem endpointów, payloadów, uprawnień i filtrów.
Wspólny runtime `assets/js/ui.js` obsługuje natywne `<dialog>`, menu Popover API,
klawiaturę, zakładki, podpowiedzi, powiadomienia i dostępność dynamicznego HTML.
`shell.js` zapewnia nawigację i wariant mobilny. `workspace.css` definiuje tokeny i komponenty.

Tom Select, flatpickr, Chart.js, heic2any i ikony Lucide są dostarczane lokalnie z przypiętymi wersjami
w `frontend/package-lock.json`. Brak wymaganego CDN czy serwera Node w produkcji.
Odtworzenie bibliotek: `npm ci --prefix frontend && npm run vendor --prefix frontend`.
Gotowe pliki statyczne są częścią JAR; zwykły build Maven wystarczy do wdrożenia.

## Ekrany / zakres funkcjonalny

| Ekran | Funkcje zachowane |
| --- | --- |
| index | filtry i ich stan, paginacja, ceny, dane telefonu, przyjęcie, edycja, model, przekazanie, usunięcie/przywrócenie, rezerwacje, koszyk |
| addPhones | OCR zdjęć, wiele plików, drag and drop, ręczne dane/model, warianty, duplikaty i zapis hurtowy |
| invoice | koszyk, ręczne pozycje netto, VAT, data, gwarancja, używany, notatki, PDF |
| receipts | filtry, paginacja, PDF, faktura serwisowa, anulowanie, poprawa dla administratora |
| receipt-edit | daty, kwoty, VAT, PDF z zapisem i bez zapisu |
| repair | tablica i mobilne zakładki, drag and drop, formularze, klienci, punkty serwisowe, statusy, koszty, zaliczki, notatki, PDF, przywrócenie do sklepu |
| serviceHistory | filtrowanie dat/lokalizacji/klienta, szczegóły, PDF, przywracanie, link do zlecenia |
| clients | wyszukiwanie, paginacja, dodawanie, edycja, usuwanie |
| offers | filtry, paginacja, wybór telefonu, dane, zdjęcia i ich kolejność, zapis, edycja, usuwanie |
| purchase | filtry, szczegóły, zdjęcia, komentarze, status, wycena, zamknięcie |
| modelsBase | filtry, paginacja, specyfikacja, warianty, dodawanie, edycja, usuwanie |
| reports | wyniki dnia, stan magazynu, zakresy dat, sprzedaż, zysk na wykresie, serwis |
| settings | lokalizacja, aktywacja użytkowników, przypisanie i tryb lokalizacji |
| login | logowanie, rejestracja, reset hasła, aktywacja konta wg dotychczasowego API |
| 404 / 500 | nawigacja, wyjaśnienie i powrót do magazynu |

`screen-manifest.json` zapisuje kompletną listę ekranów i identyfikatory pól oryginału
na potrzeby testu zgodności. Przyjęcie urządzeń (OCR i wpis ręczny) jest dostępne na dedykowanym ekranie `addPhones.html` przez przycisk „Dodaj telefony” w magazynie. Zmiany backendu ograniczają się do uprawnień do nowych
zasobów statycznych. Endpointy oraz modele API pozostają bez zmian.

## Sprawdzanie

```sh
npm ci --prefix src/test/frontend
cd src/test/frontend && npx playwright install chromium && npm test
```

80 testów przeglądarkowych obejmuje oba interfejsy, wszystkie 16 nowych ekranów przy
szerokościach 375 i 1440 px, dialogi, uprawnienia, operacje formularzy, dokumenty/PDF,
OCR multipart, koszyk, serwis, raporty, logowanie/wylogowanie i puste listy.

Testy Java bez wymagającego skonfigurowanej bazy testu startu aplikacji:
`./mvnw '-Dtest=!GsmSellerApplicationTests' package`.
Pełne `./mvnw test` uruchamia dodatkowo istniejący `contextLoads`, który wymaga konfiguracji
źródła danych; bez niej kończy się komunikatem `Failed to determine a suitable driver class`.

Nowe adresy `/v2/` kierują niezalogowanego użytkownika na `/v2/login.html`, klasyczne
adresy zachowują `/login.html`. Wylogowanie nowego widoku korzysta z istniejącego `/logout`.

Testy używają lokalnego Chromium i kontrolowanych odpowiedzi API; nie modyfikują danych produkcyjnych.
