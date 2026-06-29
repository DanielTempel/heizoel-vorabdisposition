# Frontend-Dokumentation

Diese Dokumente beschreiben die lokale Arbeit mit dem React-Frontend der Kundenseite zur Terminrückmeldung.

## Dokumente

| Dokument | Inhalt |
| --- | --- |
| [Mock-Modus](./mock-mode.md) | Lokale Frontend-Szenarien ohne Backend, Datenbank oder Postman. |
| [Backend-Flow](./backend-flow.md) | Ende-zu-Ende-Test mit Docker, Backend, Postman, Mailpit und echtem Bestätigungslink. |

## Modi

Das Frontend kann in zwei Modi betrieben werden:

- Backend-Modus: Standardmodus. Das Frontend ruft die echten Backend-APIs auf.
- Mock-Modus: Das Frontend verwendet lokale Testdaten anhand des Tokens in der URL.

Wenn `VITE_CONFIRMATION_API_MODE` nicht gesetzt ist, wird automatisch der Backend-Modus verwendet.

## Lokaler Start

```powershell
cd frontend
npm install
npm run dev
```

Das Frontend ist anschließend unter folgender Adresse erreichbar:

```text
http://localhost:3000
```

Nach Änderungen an `.env.local` muss der Vite-Dev-Server neu gestartet werden.

## Typische Verwendung

Für schnelle UI-Prüfungen wird der Mock-Modus verwendet. Damit können einzelne Zustände der Kundenseite direkt über feste URLs geöffnet werden.

Für eine vollständige Prüfung des Systems wird der Backend-Modus verwendet. Dabei wird über Postman eine echte Rückmeldeanfrage erstellt, der Bestätigungslink aus Mailpit geöffnet und anschließend die Antwort des Kunden im Frontend getestet.
