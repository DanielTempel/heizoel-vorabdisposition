# Frontend Mock-Modus

Die Kundenseite zur Terminrückmeldung kann in zwei Modi betrieben werden:

- Backend-Modus: Das Frontend ruft die echten Backend-APIs auf.
- Mock-Modus: Das Frontend liefert lokale Testdaten anhand des Tokens in der URL.

Der Backend-Modus ist der Standard. Wenn `VITE_CONFIRMATION_API_MODE` nicht gesetzt ist, verwendet das Frontend automatisch das Backend.

## Mock-Modus aktivieren

Datei `frontend/.env.local` erstellen oder bearbeiten:

```env
VITE_CONFIRMATION_API_MODE=mock
VITE_API_BASE_URL=http://localhost:8080
```

Nach einer Änderung an `.env.local` muss der Vite-Dev-Server neu gestartet werden:

```powershell
cd frontend
npm run dev
```

`frontend/.env.local` wird von git ignoriert. Dadurch kann jede Person lokal selbst entscheiden, ob sie mit Mock-Daten oder mit dem Backend arbeitet.

## Mock-Modus deaktivieren

Die Mock-Variable auskommentieren oder entfernen:

```env
# VITE_CONFIRMATION_API_MODE=mock
# VITE_API_BASE_URL=http://localhost:8080
```

Nach einem Neustart des Frontends wird wieder der Backend-Modus verwendet.

## Verfuegbare Mock-URLs

Diese URLs können im Mock-Modus direkt im Browser geöffnet werden:

```text
http://localhost:3000/confirmation/mock-sent
http://localhost:3000/confirmation/mock-confirmed
http://localhost:3000/confirmation/mock-rejected
http://localhost:3000/confirmation/mock-no-response
http://localhost:3000/confirmation/mock-error
http://localhost:3000/confirmation/mock-no-tracking
http://localhost:3000/confirmation/mock-arrived
http://localhost:3000/confirmation/mock-driver-error
```

## Szenario-Übersicht

| Token | Zweck |
| --- | --- |
| `mock-sent` | Offene Rückmeldeanfrage mit Buttons zum Bestätigen oder Ablehnen. |
| `mock-confirmed` | Bereits bestätigte Anfrage. |
| `mock-rejected` | Bereits abgelehnte Anfrage. |
| `mock-no-response` | Abgelaufene Anfrage mit Status `NO_RESPONSE`. |
| `mock-error` | Fehlgeschlagener Abruf der Termindaten, um die Fehlerseite zu testen. |
| `mock-no-tracking` | Bestätigte Anfrage, bei der Tracking noch nicht verfügbar ist. |
| `mock-arrived` | Bestätigte Anfrage, bei der das Fahrzeug bereits am Ziel angekommen ist. |
| `mock-driver-error` | Bestätigte Anfrage, bei der die Fahrerposition nicht aktualisiert werden kann. |

## Hinweise

Mock-Tokens sind keine echten Backend-Tokens. Sie funktionieren nur, wenn `VITE_CONFIRMATION_API_MODE=mock` aktiv ist.

Für einen Ende-zu-Ende-Test wird der Backend-Modus verwendet. Dafür wird eine echte Rückmeldeanfrage über Postman oder einen anderen HTTP-Client erstellt. Anschließend wird der echte Bestätigungslink aus Mailpit im Browser geöffnet.
