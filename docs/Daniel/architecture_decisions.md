## Entscheidung: Kundenrückmeldung über tokenbasierte Aktionsbuttons

Für die digitale Rückbestätigung geplanter Lieferfenster werden in der Kundenbenachrichtigung sichtbare Aktionsbuttons verwendet, z. B.:

- Liefertermin bestätigen
- Termin passt nicht

Diese Buttons ändern den Rückbestätigungsstatus jedoch nicht direkt beim Öffnen des Links. Technisch handelt es sich um tokenbasierte Action-Links, die auf eine minimale Kundenseite führen.

Auf dieser Kundenseite sieht der Kunde die relevanten Lieferinformationen und bestätigt seine Auswahl final durch eine bewusste Aktion. Erst diese finale Aktion ändert den Status im Backend.

## Begründung

Eine direkte Statusänderung über einen einfachen Link wäre technisch riskant, da E-Mail-Clients, Sicherheitssoftware oder Link-Scanner Links automatisch öffnen können. Dadurch könnte ein Auftrag versehentlich bestätigt oder abgelehnt werden, ohne dass der Kunde bewusst gehandelt hat.

Durch die minimale Bestätigungsseite bleibt der Ablauf für den Kunden sehr einfach, gleichzeitig wird eine unbeabsichtigte Statusänderung vermieden.

## Zielbild

Nachricht an den Kunden:

- Button: Liefertermin bestätigen
- Button: Termin passt nicht

Technischer Ablauf:

1. Kunde klickt auf einen Aktionsbutton.
2. Die minimale Kundenseite wird geöffnet.
3. Die gewählte Aktion ist bereits vorausgewählt.
4. Der Kunde sieht die Lieferdaten.
5. Der Kunde bestätigt die Aktion final.
6. Erst danach aktualisiert das Backend den Rückbestätigungsstatus.

## Gültigkeit für Kommunikationskanäle

Dieses Konzept ist kanalübergreifend verwendbar:

- E-Mail
- SMS
- WhatsApp

Im MVP wird E-Mail umgesetzt. SMS und WhatsApp werden als mögliche Erweiterungen betrachtet.

## Konsequenz für die API

Das Backend generiert tokenbasierte Action-URLs, z. B.:

- `confirmActionUrl`
- `rejectActionUrl`

Diese URLs dienen zur Öffnung der Kundenseite. Die eigentliche Statusänderung erfolgt nicht durch den GET-Aufruf der URL, sondern durch einen expliziten Submit auf der Kundenseite.

Vorgang:
1. DISPO ruft Backend direkt per REST API auf.
2. Button befindet sich fachlich im DISPO.
3. Im Prototyp kann der DISPO-Aufruf über Swagger/Postman/Mock simuliert werden.
4. Keine Tour-Daten in v1.
5. Kein customerNumber.
6. Kein pricePer100Liters.
7. Kein dispatcherId / dispatcherName.
8. deliveryAddress bleibt ein einfacher String.
9. Backend sendet E-Mail synchron.
10. 201 Created nur bei erfolgreicher Anfrage + erfolgreichem Mailversand.
11. 502 Bad Gateway bei Mailversandfehler.
12. E-Mail enthält Aktionsbuttons.
13. Aktionsbuttons öffnen minimale Kundenseite.
14. Statusänderung erst nach finalem Submit auf der Kundenseite.