# Frontend Backend-Flow

Dieses Dokument beschreibt, wie die Kundenseite mit echten Backend-Daten geprüft wird. Postman übernimmt dabei die Rolle des externen DISPO-Systems und erstellt eine neue Rückmeldeanfrage.

## Voraussetzungen

- Docker Desktop ist gestartet.
- Das Backend kann lokal mit Maven gestartet werden.
- Das Frontend ist installiert und lauffähig.
- Für Postman wird bei lokalen URLs der Desktop Agent oder die Postman Desktop App benötigt.

## 1. Frontend auf Backend-Modus stellen

Im Backend-Modus muss `VITE_CONFIRMATION_API_MODE=mock` entfernt oder auskommentiert sein.

Beispiel `frontend/.env.local`:

```env
# VITE_CONFIRMATION_API_MODE=mock
# VITE_API_BASE_URL=http://localhost:8080
```

Wenn `VITE_CONFIRMATION_API_MODE` nicht gesetzt ist, verwendet das Frontend automatisch das Backend.

Nach einer Änderung an `.env.local` muss das Frontend neu gestartet werden.

## 2. Infrastruktur starten

Aus dem Backend-Verzeichnis:

```powershell
cd backend
docker compose up -d
```

Dadurch werden unter anderem PostgreSQL, Mailpit und DISPO-Mock gestartet.

Wichtige lokale URLs:

| Dienst | URL |
| --- | --- |
| Backend | `http://localhost:8080` |
| Frontend | `http://localhost:3000` |
| Mailpit | `http://localhost:8025` |
| DISPO-Mock | `http://localhost:8090` |

## 3. Backend starten

Aus dem Backend-Verzeichnis:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Das Backend läuft, sobald im Log eine Meldung ähnlich zu dieser erscheint:

```text
Tomcat started on port 8080
Started BackendApplication
```

## 4. Frontend starten

In einem zweiten Terminal:

```powershell
cd frontend
npm run dev
```

## 5. Rückmeldeanfrage mit Postman erstellen

In Postman:

- Methode: `POST`
- URL: `http://localhost:8080/api/dispo/confirmation-requests`
- Body: `raw`
- Format: `JSON`

Beispiel:

```json
{
  "externalOrderId": "A-FRONTEND-001",
  "customerName": "Max Muller",
  "communicationChannel": "EMAIL",
  "customerEmail": "example@example.com",
  "customerPhoneNumber": null,
  "deliveryAddress": "Domstrasse 40, 97070 Würzburg",
  "product": "Heizöl Standard",
  "quantityLiters": 3000,
  "deliveryDate": "2026-06-29",
  "deliveryWindowStart": "10:00",
  "deliveryWindowEnd": "11:00",
  "responseDeadlineHours": 24,
  "priceDisplayText": "100 EUR"
}
```

Für einen Tracking-Test muss `deliveryDate` auf den aktuellen Tag gesetzt werden. Sonst liefert das Backend `trackingAvailable=false`.

Bei erfolgreicher Erstellung antwortet das Backend mit `201 Created`:

```json
{
  "externalOrderId": "A-FRONTEND-001",
  "confirmationStatus": "SENT"
}
```

Für weitere Tests sollte `externalOrderId` geändert werden, zum Beispiel `A-FRONTEND-002`, damit eine neue Anfrage erzeugt wird.

## 6. Bestätigungslink aus Mailpit öffnen

Mailpit öffnen:

```text
http://localhost:8025
```

Die E-Mail enthält einen Link nach diesem Muster:

```text
http://localhost:3000/confirmation/{token}
```

Dieser Link öffnet die Kundenseite mit echten Backend-Daten.

## 7. Kundenantwort prüfen

Auf der Kundenseite kann der Liefertermin bestätigt oder abgelehnt werden.

Nach der Antwort kann der DISPO-Mock geprüft werden:

- Methode: `GET`
- URL: `http://localhost:8090/api/dispo/confirmation-status-updates`

Dort sollte ein Callback mit Status `CONFIRMED` oder `REJECTED` sichtbar sein.

## Hinweise

Der Backend-Flow prüft das Zusammenspiel von Frontend, Backend, Datenbank, E-Mail-Versand und DISPO-Callback. Für reine UI-Arbeit ist der [Mock-Modus](./mock-mode.md) schneller.
