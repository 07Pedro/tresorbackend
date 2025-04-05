Dokumentation tresor-app Petr Cerny
---

## Passwort Hashing und Verifizierung

### Funktionsweise

#### Passwort Hashing
- Beim Hashing wird das Benutzerpasswort zusammen mit einem **Pepper** kombiniert, um die Sicherheit zu erhöhen.
- Das Passwort wird mit **BCrypt** gehasht, und in der Datenbank zu speichern.
- Der **Pepper** wird als zusätzlicher Sicherheitsfaktor dem Passwort hinzugefügt, bevor es gehasht wird.

#### Passwort Verifizierung
- Bei der Verifizierung wird das eingegebene Passwort erneut mit dem **Pepper** kombiniert und mit dem gespeicherten Hash verglichen.
- Wird das kombinierte Passwort mit dem gespeicherten Hash übereingestimmt, gilt das Passwort als korrekt.

#### Sicherheit
- Der **Pepper** schützt das Passwort zusätzlich zum Hashing.


## Benutzer-Login-Implementierung

### Funktionsweise

#### Frontend (React)
- Der Benutzer gibt E-Mail und Passwort ein.
- Diese Daten werden über eine **POST-Anfrage** an das Backend gesendet.
- Bei Erfolg wird der Benutzer weitergeleitet, bei Fehlschlag erfolgt eine Fehlermeldung.

#### Backend (Spring Boot)
- Das Backend sucht den Benutzer anhand der **E-Mail** und vergleicht das eingegebene Passwort mit dem gespeicherten Hash.
- Erfolgt die Übereinstimmung, wird eine Erfolgsmeldung zurückgegeben.

#### Passwortverifizierung
- Das eingegebene Passwort wird mit einem **Pepper** kombiniert und gehasht, um zusätzliche Sicherheit zu gewährleisten.

#### CORS-Konfiguration
- CORS wird aktiviert, um den Zugriff vom Frontend (z. B. `localhost:3000`) auf das Backend (z. B. `localhost:8080`) zu ermöglichen.
