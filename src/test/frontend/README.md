# Regresje mobilnego interfejsu

Testy uruchamiają lokalny HTML, CSS, JavaScript i Materialize w Chromium z emulacją
telefonu. API jest symulowane; testy nie łączą się z backendem ani produkcją.
Inicjalizatory strony zależne od CDN/TomSelect są pomijane. Testujemy rzeczywistą
ścieżkę pobierania i renderowania listy oraz obsługę akcji.

```sh
cd src/test/frontend
npm ci
npx playwright install chromium
npm test
```
