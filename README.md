# NoteV2 – Fullstack-Notizen-App

Eine Fullstack-Webanwendung zum Erstellen und Verwalten persönlicher Notizen. Die Anwendung bietet eine JWT-basierte Authentifizierung, Refresh-Token-Rotation, CSRF-Schutz und eine benutzerbezogene Zugriffskontrolle.

Das Projekt verbindet Backend-Entwicklung mit IT-Sicherheit, automatisierten Tests, Containerisierung und Cloud-Infrastruktur.

## Funktionen

- Registrierung und Anmeldung von Benutzern
- JWT-basierte Authentifizierung
- Refresh-Token mit Rotation und Widerruf
- CSRF-Schutz für schreibende API-Anfragen
- Multi-User-System mit strikt getrennten Notizen
- Notizen erstellen und löschen
- Geschützte REST-API-Endpunkte mit Spring Security
- Rate Limiting für Login und Registrierung
- Eingabevalidierung mit Bean Validation
- Zentrale Fehlerbehandlung
- Datenbankmigrationen mit Flyway
- Automatisierte Tests mit JUnit, Mockito und MockMvc
- Containerisierte Entwicklungsumgebung mit Docker
- CI/CD mit GitHub Actions
- Infrastrukturverwaltung mit Terraform

## Technologie-Stack

| Bereich | Technologien |
|---|---|
| Backend | Java 25, Spring Boot 4, Spring Security 7 |
| Authentifizierung | JWT, BCrypt, Refresh Tokens |
| Datenbank | PostgreSQL, Spring Data JPA, Hibernate |
| Datenbankmigrationen | Flyway |
| Validierung | Jakarta Bean Validation |
| Rate Limiting | Bucket4j |
| Frontend | React, Vite, Tailwind CSS |
| HTTP-Kommunikation | Axios |
| Tests | JUnit 5, Mockito, MockMvc, H2 |
| Containerisierung | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Infrastruktur | Terraform, Azure Container Apps, Azure Database for PostgreSQL Flexible Server |
| Lasttests | Grafana k6 |

## Architektur

Die Anwendung folgt einer mehrschichtigen Backend-Architektur mit klarer Trennung zwischen HTTP-Verarbeitung, Geschäftslogik und Datenbankzugriff.

```text
React-Frontend
      |
      | REST API (Axios)
      |
Spring-Boot-Backend
      |
      +-- Sicherheitsschicht
      |      +-- JWT-Authentifizierung
      |      +-- CSRF-Schutz
      |      +-- Rate Limiting
      |
      +-- Controller-Schicht
      |
      +-- Service-Schicht
      |
      +-- Repository-Schicht
      |
PostgreSQL-Datenbank
```

### Backend-Architektur

- **Controller-Schicht:** Nimmt HTTP-Anfragen entgegen und stellt API-Antworten bereit.
- **Service-Schicht:** Enthält die Geschäftslogik und die Authentifizierungsabläufe.
- **Repository-Schicht:** Übernimmt den Datenbankzugriff mit Spring Data JPA.
- **Sicherheitsschicht:** Zuständig für Authentifizierung, Autorisierung, CSRF-Schutz und Rate Limiting.
- **DTO-Validierung:** Prüft eingehende Daten mit Bean Validation.
- **Zentrale Fehlerbehandlung:** Einheitliche Fehlerantworten über einen `GlobalExceptionHandler`.
- **Datenbankmigrationen:** Änderungen am Datenbankschema werden versioniert mit Flyway verwaltet.

## Sicherheitskonzept

Ein besonderer Schwerpunkt des Projekts liegt auf der Absicherung der Authentifizierung und der API-Endpunkte.

### JWT-Authentifizierung und Token-Verwaltung

Die Anwendung verwendet ein kurzlebiges JWT-Access-Token in Kombination mit einem länger gültigen Refresh-Token.

| Komponente | Umsetzung |
|---|---|
| Access Token | JWT |
| Gültigkeit des Access Tokens | 15 Minuten |
| Speicherung des Access Tokens | `localStorage` im Browser |
| Refresh Token | Kryptografisch zufällig generierter Token |
| Gültigkeit des Refresh Tokens | 7 Tage |
| Speicherung im Browser | `HttpOnly`-Cookie |
| Speicherung in der Datenbank | SHA-256-Hash |
| Passwortspeicherung | BCrypt-Hash |

Ablauf der Token-Verwaltung:

1. Nach erfolgreicher Anmeldung stellt das Backend ein Access Token aus und setzt ein Refresh-Token-Cookie.
2. Das Frontend sendet das Access Token bei geschützten API-Anfragen im `Authorization: Bearer`-Header.
3. Läuft das Access Token ab, fordert das Frontend über `/auth/refresh` ein neues Access Token an.
4. Das Backend prüft das Refresh Token und führt eine Token-Rotation durch.
5. Das bisherige Refresh Token wird widerrufen und durch ein neues ersetzt.
6. Beim Logout wird das Refresh Token widerrufen und das Cookie gelöscht.

Das ursprüngliche Refresh Token wird nicht im Klartext in der Datenbank gespeichert. Stattdessen wird ein SHA-256-Hash abgelegt.

### CSRF-Schutz

Für den Schutz vor Cross-Site-Request-Forgery-Angriffen wird die SPA-Unterstützung von Spring Security verwendet.

- CSRF-Schutz ist über `csrf.spa()` aktiviert.
- Das CSRF-Token wird über ein `XSRF-TOKEN`-Cookie bereitgestellt.
- Das Frontend liest den CSRF-Token aus dem Cookie.
- Axios überträgt ihn im HTTP-Header `X-XSRF-TOKEN`.
- Schreibende Anfragen, beispielsweise `POST` und `DELETE`, benötigen einen gültigen CSRF-Token.
- Anfragen ohne gültigen CSRF-Token werden mit HTTP 403 abgelehnt.
- `GET`-Anfragen benötigen keinen CSRF-Token.

Das Refresh Token liegt in einem `HttpOnly`-Cookie und kann daher nicht direkt durch JavaScript ausgelesen werden.

### Weitere Sicherheitsmaßnahmen

- Passwort-Hashing mit BCrypt
- Prüfung von JWT-Signatur und Ablaufzeit
- Benutzerbezogene Zugriffskontrolle auf Notizen
- Rate Limiting für `/auth/login` und `/auth/register`
- Bucket4j mit Token-Bucket-Algorithmus
- Eingabevalidierung mit Jakarta Bean Validation
- Zentrale Fehlerbehandlung
- Verwaltung von Secrets über Umgebungsvariablen

> **Hinweis zum Produktionsbetrieb:** Die Sicherheitsfunktionen sind implementiert und teilweise durch automatisierte Tests sowie manuelle Browser-Tests überprüft. Die vollständige Produktionshärtung steht noch aus. Dazu gehören insbesondere die abschließende Prüfung von `Secure`- und `SameSite`-Cookie-Einstellungen, HTTPS, CORS und der Behandlung von Client-IP-Adressen hinter Reverse Proxys.

## REST-API

### Authentifizierung

| Methode | Endpunkt | Beschreibung |
|---|---|---|
| POST | `/auth/register` | Registrierung eines neuen Benutzers |
| POST | `/auth/login` | Anmeldung und Ausstellung der Tokens |
| POST | `/auth/refresh` | Erneuerung des Access Tokens und Rotation des Refresh Tokens |
| POST | `/auth/logout` | Widerruf des Refresh Tokens und Löschen des Cookies |
| GET | `/auth/me` | Abruf des aktuell angemeldeten Benutzers |

### Notizen

| Methode | Endpunkt | Beschreibung |
|---|---|---|
| GET | `/notes` | Alle Notizen des angemeldeten Benutzers abrufen |
| POST | `/notes` | Neue Notiz erstellen |
| DELETE | `/notes/{id}` | Notiz löschen |

Geschützte Endpunkte benötigen ein gültiges Access Token. Schreibende Anfragen werden zusätzlich durch CSRF-Prüfungen geschützt.

## Tests und Qualitätssicherung

Für die Qualitätssicherung werden Unit- und Integrationstests mit JUnit 5, Mockito, MockMvc und H2 eingesetzt.

Die Tests decken unter anderem folgende Bereiche ab:

- Registrierung und Anmeldung
- Ungültige Zugangsdaten und fehlerhafte Eingaben
- Erstellung und Validierung von Refresh Tokens
- Refresh-Token-Rotation und Widerruf
- Abgelaufene und ungültige Refresh Tokens
- Logout und Cookie-Verarbeitung
- JWT-Authentifizierung und Zugriffskontrolle
- Benutzerbezogene Trennung von Notizen
- `POST`- und `DELETE`-Anfragen mit gültigem CSRF-Token
- Ablehnung von Anfragen ohne CSRF-Token mit HTTP 403
- `GET`-Anfragen ohne CSRF-Token

### Tests ausführen

Im Backend-Verzeichnis:

```bash
./mvnw test
```

Unter Windows mit PowerShell:

```powershell
.\mvnw.cmd test
```

## Projektstruktur

```text
notev2/
├── .github/
│   └── workflows/          GitHub Actions CI/CD
├── docs/                   Screenshots und Dokumentation
├── infra/                  Terraform-Infrastruktur
├── notev2-backend/
│   ├── src/
│   └── pom.xml
├── notev2-frontend/
│   ├── src/
│   └── package.json
├── compose.yml
├── load-test.js
└── README.md
```

## Lokale Einrichtung

### Voraussetzungen

- Java 25
- Node.js und npm
- Docker und Docker Compose
- Git
- PostgreSQL (bei manueller Ausführung ohne Docker)

### Repository klonen

```bash
git clone https://github.com/vithujan-suthahar/notev2-app.git
cd notev2-app
```

### Option A: Docker Compose

Die Anwendung kann direkt über Docker Compose gestartet werden:

```bash
docker compose up --build
```

### Option B: Manuell

#### 1. Datenbank starten

Eine PostgreSQL-Instanz muss verfügbar sein. Die Datenbankverbindung wird über die Backend-Konfiguration eingerichtet.

#### 2. Backend starten

Linux / macOS:

```bash
cd notev2-backend
export JWT_SECRET="your_secure_secret_here"
./mvnw spring-boot:run
```

Windows (PowerShell):

```powershell
cd notev2-backend
$env:JWT_SECRET="your_secure_secret_here"
.\mvnw.cmd spring-boot:run
```

#### 3. Frontend starten

```bash
cd notev2-frontend
npm install
npm run dev
```

Die Standard-API-Adresse des Frontends lautet:

```text
http://localhost:8080
```

Sie kann über die Umgebungsvariable `VITE_API_URL` angepasst werden.

## Umgebungsvariablen

Das Backend benötigt mindestens die Umgebungsvariable `JWT_SECRET`.

- Für `JWT_SECRET` muss ein ausreichend starker, zufällig generierter Schlüssel verwendet werden.
- Secrets dürfen niemals in das Git-Repository eingecheckt werden.
- Zugangsdaten für Entwicklung und Produktion sollten getrennt verwaltet werden.

## CI/CD und Infrastruktur

- **GitHub Actions** führt automatisierte Backend-Tests aus.
- **Docker** und **Docker Compose** stellen eine containerisierte lokale Entwicklungsumgebung bereit.
- **Terraform** verwaltet die Azure-Infrastruktur als Infrastructure as Code.
- **Azure Container Apps** dienen zum Betrieb der Frontend- und Backend-Container.
- **Azure Database for PostgreSQL Flexible Server** stellt die Datenbank bereit.

## Performance-Tests

Die REST-API wurde lokal mit Grafana k6 unter Last getestet.

### Testumgebung

| Komponente | Konfiguration |
|---|---|
| Backend | Spring Boot 4, Java 25 |
| Datenbank | PostgreSQL in Docker |
| Testwerkzeug | Grafana k6 |
| Testdauer | 30 Sekunden pro Szenario |
| Testsystem | Lokaler Entwicklungsrechner |

### Ergebnisse – `/notes`

Der Endpunkt `/notes` wurde mit authentifizierten Anfragen und ohne Rate Limiting getestet.

| Kennzahl | 100 Virtual Users | 500 Virtual Users |
|---|---|---|
| Dauer | 30 s | 30 s |
| Anfragen | 109.213 | 101.353 |
| Anfragen pro Sekunde | 3.638 | 3.365 |
| Durchschnittliche Antwortzeit | 27,38 ms | 146,98 ms |
| p95-Antwortzeit | 83,56 ms | 331,69 ms |
| Maximale Antwortzeit | 282 ms | 1,43 s |
| Fehlerrate | 0 % | 0,64 % |

Bei 500 Virtual Users stieg die CPU-Auslastung des Spring-Boot-Containers auf etwa 450 % (entspricht ungefähr 4,5 CPU-Kernen). Die PostgreSQL-Datenbank erreichte eine CPU-Auslastung von etwa 100 %.

Die steigenden Antwortzeiten und die höhere Ressourcenauslastung zeigen, dass Datenbankverbindungen und Datenbankverarbeitung bei höherer Parallelität zu Engpässen werden können.

### Ergebnisse – `/auth/login`

Der Login-Endpunkt wurde mit aktiviertem Rate Limiting getestet. Die Konfiguration erlaubt fünf Anfragen pro Minute und IP-Adresse.

| Kennzahl | 100 Virtual Users |
|---|---|
| Dauer | 30 s |
| Anfragen | 359.731 |
| Anfragen pro Sekunde | ca. 11.990 |
| Durchschnittliche Antwortzeit | 8,14 ms |
| p95-Antwortzeit | 15,38 ms |
| Abgelehnte Anfragen (HTTP 429) | ca. 99,997 % |

Der Test zeigt, dass der Rate Limiter übermäßige Anfragen frühzeitig ablehnt, bevor die eigentliche Authentifizierungslogik und der Datenbankzugriff ausgeführt werden. Die Messung zeigt damit hauptsächlich das Verhalten des Rate Limiters unter Last und ist kein Benchmark für erfolgreiche Login-Vorgänge.

Die angegebenen Messwerte stammen aus einer lokalen Testumgebung und stellen keine Produktionskapazitätsmessung dar.

### Lasttest ausführen

```bash
k6 run load-test.js
```

## Azure-Deployment

Die Anwendung wurde bereits auf Azure bereitgestellt.

Verwendete Azure-Komponenten:

- Azure Container Apps für Frontend und Backend
- Azure Database for PostgreSQL Flexible Server für die Datenbank
- Terraform zur Bereitstellung und Verwaltung der Infrastruktur

## Screenshots der Anwendung

### Anmeldung

![Anmeldung](docs/login_default.png)

![Anmeldung mit Fehlermeldung](docs/login_error.png)

### Registrierung

![Registrierung](docs/registrieren_default.png)

![Registrierung mit Fehlermeldung](docs/registrieren_error.png)

### Rate Limiting

![Rate Limiting](docs/rate_limit_error.png)

### Notizen-App

![Notizen-App](docs/notes.png)

### Azure-Infrastruktur

![Azure-Dashboard](docs/azure_dashboard.png)

### Lasttest in der Cloud

![Cloud-Lasttest](docs/cloud_load_test.png)

## Beobachtungen aus Cloud-Lasttests

- Bei `min_replicas=0` dauerte die erste Anfrage nach einer Inaktivitätsphase ungefähr 45 Sekunden.
- Bei einem bereits gestarteten Container lag die durchschnittliche Antwortzeit anschließend bei etwa 109 ms.
- Hinter dem Azure-Reverse-Proxy lieferte `request.getRemoteAddr()` die IP-Adresse des Proxys statt der tatsächlichen Client-IP.
- Dadurch wurden Anfragen verschiedener Clients effektiv demselben Rate-Limit-Bucket zugeordnet.
- Die vertrauenswürdige Auswertung weitergeleiteter Client-IP-Adressen ist eine geplante Erweiterung.

## Geplante Erweiterungen

- Notizen bearbeiten
- Sichere Client-IP-Erkennung hinter Reverse Proxys für das Rate Limiting
- Redis-basiertes Rate Limiting für horizontale Skalierung
- Weitere Prüfung und Härtung der Produktionskonfiguration
- Responsives Design verbessern
- Dark Mode

## Autor

Vithujan Suthahar

GitHub: [vithujan-suthahar](https://github.com/vithujan-suthahar)
