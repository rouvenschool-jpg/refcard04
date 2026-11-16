# Architecture Ref. Card 04

Spring-Boot-Anwendung, mit der Dateien in einen **privaten AWS S3 Bucket** hochgeladen
und über zeitlich begrenzte Download-Links (Presigned URLs) wieder geöffnet werden können.

## Voraussetzungen

- JDK 8 oder neuer (getestet mit JDK 21)
- Ein AWS-Account mit einem S3 Bucket (siehe unten)

## 1. S3 Bucket anlegen

1. In der AWS-Konsole unter S3 einen Bucket erstellen.
   - Der Name muss **global eindeutig** sein (Standard: `s3-ref-card-04-rl`).
   - Region: `us-east-1` (Standard) oder die Region, die in `AWS_REGION` gesetzt wird.
2. **"Block all public access" aktiviert lassen.** Der Bucket bleibt privat, Downloads
   laufen über Presigned URLs.
3. Die verwendeten Zugangsdaten brauchen diese Rechte auf den Bucket (im AWS Academy
   Learner Lab sind sie in der Regel bereits vorhanden):

   | Berechtigung       | Wofür                 |
   |--------------------|-----------------------|
   | `s3:ListBucket`    | Dateiliste anzeigen   |
   | `s3:PutObject`     | Datei hochladen       |
   | `s3:GetObject`     | Datei herunterladen   |
   | `s3:DeleteObject`  | nur für `deleteFile()`|

## 2. Konfiguration

Die Werte werden über Umgebungsvariablen gesetzt (Standardwerte in `application.properties`):

| Variable                | Bedeutung                              | Standard            |
|-------------------------|----------------------------------------|---------------------|
| `S3_BUCKET`             | Name des Buckets                       | `s3-ref-card-04-rl` |
| `AWS_REGION`            | Region des Buckets                     | `us-east-1`         |
| `AWS_ACCESS_KEY_ID`     | Zugangsdaten                           | –                   |
| `AWS_SECRET_ACCESS_KEY` | Zugangsdaten                           | –                   |
| `AWS_SESSION_TOKEN`     | nur bei temporären Zugangsdaten (z. B. AWS Academy) | –      |

Statt der Umgebungsvariablen liest das AWS SDK auch `~/.aws/credentials`.

Im Repository liegt die Vorlage `env-and-start-example.sh`. Daraus erstellt man eine eigene,
lokale Datei `env-and-start.sh` und trägt dort die Werte ein. Diese Datei steht in
`.gitignore` und wird **nicht** committet:

```bash
cp env-and-start-example.sh env-and-start.sh
# env-and-start.sh öffnen und AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY,
# AWS_SESSION_TOKEN und S3_BUCKET eintragen
```

Die Vorlage setzt die Umgebungsvariablen, baut das Projekt und startet die Anwendung:

```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_SESSION_TOKEN=...
export S3_BUCKET=...
export AWS_REGION=us-east-1

mvn package
java -jar target/architecture-refcard-04-0.0.1-SNAPSHOT.jar
```

> Zugangsdaten gehören nie ins Repository. Temporäre Zugangsdaten laufen nach einigen
> Stunden ab, danach müssen sie in `env-and-start.sh` ersetzt und die Anwendung neu
> gestartet werden.

## 3. Bauen und starten (JAR)

Lokal reicht:

```bash
source env-and-start.sh
```

Das entspricht diesen einzelnen Schritten:

```bash
./mvnw package                     # baut target/architecture-refcard-04-0.0.1-SNAPSHOT.jar
source env-and-start.sh            # setzt die Variablen (und startet, siehe oben)
java -jar target/architecture-refcard-04-0.0.1-SNAPSHOT.jar
```

Danach ist die Anwendung unter http://localhost:8080 erreichbar.
Einen anderen Port setzt man mit `--server.port=9090`.

Zum schnellen Ausprobieren während der Entwicklung geht auch `./mvnw spring-boot:run`
(ohne vorheriges `package`). Tests laufen mit `./mvnw test`.

## 4. Betrieb auf einer EC2-Instanz

1. **Java installieren** (Amazon Linux 2023):
   ```bash
   sudo dnf install -y java-17-amazon-corretto-headless
   ```
2. **JAR und `env-and-start.sh` auf die Instanz kopieren:**
   ```bash
   scp -i key.pem target/architecture-refcard-04-0.0.1-SNAPSHOT.jar env-and-start.sh ec2-user@<öffentliche-IP>:~
   ```
3. **Skript auf der Instanz anpassen** (`nano env-and-start.sh`):
   - Die Zeile `mvn package` löschen oder mit `#` auskommentieren. Auf der Instanz gibt es
     weder Maven noch den Quellcode, das JAR liegt schon fertig im Ordner.
   - Den Pfad im `java -jar`-Befehl anpassen (das JAR liegt direkt im Home-Ordner) und
     die App im Hintergrund starten:
     ```bash
     nohup java -jar architecture-refcard-04-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
     ```
   - Die Datei schützen: `chmod 600 env-and-start.sh`
4. **Starten** (im selben Terminal, **nicht** mit `sudo`, sonst gehen die Variablen verloren):
   ```bash
   source env-and-start.sh
   ```
5. **Security Group:** Eingehend TCP-Port **8080** erlauben.
6. Aufruf im Browser: `http://<öffentliche-IP>:8080`. Bei Problemen hilft `tail -f app.log`.

**Zugangsdaten laufen ab:** Die temporären Zugangsdaten (Session-Token) sind nur wenige
Stunden gültig. Danach die Werte in `env-and-start.sh` ersetzen, die Datei auf die Instanz
kopieren (Schritt 3 nicht vergessen), die App beenden (`pkill -f architecture-refcard`)
und neu starten (Schritt 4).

## Funktionsweise

- **Upload:** Der Dateiname wird als S3-Key verwendet. Eine Datei mit gleichem Namen
  überschreibt die bestehende.
- **Ordner:** S3 kennt keine echten Ordner, ein Ordner ist ein gemeinsames Key-Prefix
  (z. B. `bilder/katze.png`). Die Seite zeigt immer eine Ebene: zuerst die Ordner, dann die
  Dateien. Ein Klick auf einen Ordner öffnet ihn, `..` führt eine Ebene nach oben.
  Hochgeladene Dateien landen immer auf der obersten Ebene.
- **Download:** Die Links in der Dateiliste sind Presigned URLs und **10 Minuten gültig**
  (höchstens so lange wie die verwendeten Zugangsdaten). Nach Ablauf die Seite neu laden.
- **Fehler:** Probleme mit S3 (abgelaufene Zugangsdaten, fehlender Bucket, fehlende Rechte)
  werden als Meldung auf der Seite angezeigt.

## Maximale Dateigrösse

Konfiguriert in `src/main/resources/application.properties`:

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

Die Limits können in KB, MB, GB usw. angegeben werden. Grössere Dateien werden mit der
Meldung "Die Datei ist zu gross" abgelehnt.

## Häufige Fehlermeldungen

| Meldung auf der Seite | Ursache |
|---|---|
| `S3 meldet ExpiredToken` / `InvalidAccessKeyId` | Zugangsdaten abgelaufen oder falsch |
| `S3 meldet NoSuchBucket` | Bucket existiert nicht oder `S3_BUCKET` falsch |
| `S3 meldet AccessDenied` | IAM-Rechte fehlen |
| `S3 meldet PermanentRedirect` | Bucket liegt in einer anderen Region als `AWS_REGION` |
| `S3 nicht erreichbar oder keine Zugangsdaten gefunden` | Keine Credentials gesetzt oder kein Netzwerk |
