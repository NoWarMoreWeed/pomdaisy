<p align="right"><strong>🇩🇪 Deutsch</strong> · <a href="README.en.md">🇬🇧 English</a></p>

# PomDaisy: PokeDaisy auf dem iPhone und iPad

PomDaisy ist die iOS-Version von PokeDaisy. Sie liegt im Branch `ios` dieses Repos und ist ein **frühes
Experiment**: Sie läuft, aber vieles ist noch nicht auf echten Geräten geprüft. Stand: November 2026.

## Was es gibt

Die App hat im Moment **zwei Oberflächen**:

1. **Die iOS-Oberfläche (Standard beim Start).** In Swift geschrieben: Spieleliste, Spielbildschirm, Touch-Steuerung,
   Pause-Menü. Sie nutzt die gemeinsamen Kerne von PokeDaisy:
   - **GBA, Game Boy, Game Boy Color** auf mGBA, mit Companion, HUD und Schnellmenü aus dem Kern
     (der Umfang der Linux- und 3DS-Version).
   - **Nintendo DS** auf zwei Kernen, beide **ohne JIT** (iOS erlaubt im App Store und in TestFlight kein JIT):
     DSperate (Standard; auf einem iPhone 14 Pro Max etwa 40 % Auslastung) und melonDS mit **EXPAND**
     (Breitbild-Oberwelt). Umschalten im Pause-Menü.
   - Was genau geht und was fehlt, steht in [FEATURES.md](FEATURES.md).
2. **„Android-Ansicht“ (Knopf oben links).** Das ist **die echte Android-App**: derselbe Kotlin- und Compose-Code wie
   auf Android, für iOS übersetzt mit Compose Multiplatform, dazu der native Code der App (mGBA, melonDS, DSperate,
   EXPAND). Ziel ist, dass PomDaisy fast 1 zu 1 aussieht und sich anfühlt wie PokeDaisy auf Android, und dass neue
   Funktionen der Android-App automatisch mitkommen.
   - **Stand:** Bibliothek und Spielbildschirm laufen im iOS-Simulator ohne Absturz (Testprogramme für GBA und Game
     Boy). Auf einem echten iPhone ist das erst ab TestFlight-Build 32 testbar.
   - **Noch ungeprüft:** Ton, Touch-Eingaben im Spiel, Leistung, DS-Spiele.
   - **Fehlt noch:** Koop und Link-Kabel, RetroAchievements, Datei-Auswahl zum Importieren (ROMs über die Dateien-App
     ablegen, siehe unten), DS RESOLUTION (braucht OpenGL ES 3.2, das iOS nicht hat).
   - Später soll die Android-Ansicht die iOS-Oberfläche ganz ersetzen.

Beide Oberflächen benutzen dieselben Ordner. Spiele und Spielstände sind also in beiden da.

## Spiele und Spielstände

- **Keine Spiele dabei.** Du brauchst deine eigenen ROM-Dateien (`.gba`, `.gb`, `.gbc`, `.nds`). Keine BIOS-Dateien nötig.
- ROMs kommen in der Dateien-App nach **Auf meinem iPhone > PomDaisy > roms**, oder in der iOS-Oberfläche über **+**.
- Spielstände liegen in **PomDaisy > saves** (`<Spielname>.sav`), Sicherungskopien in `saves/backups`. Einen Spielstand
  von einem anderen Emulator kannst du dort ablegen, wenn er genauso heißt wie die ROM.

## Installieren

Es gibt **keinen öffentlichen Download**. Drei Wege:

1. **TestFlight-Einladung.** Der Besitzer dieses Repos kann dich als Tester eintragen. Das geht nur auf Anfrage.
2. **Selbst bauen über GitHub (für die eigene TestFlight).** Das braucht einen bezahlten Apple-Developer-Account
   (99 € im Jahr), aber keinen Mac. GitHub Actions baut auf einem macOS-Rechner:
   1. Repo forken.
   2. In `ios/App/project.yml` die `PRODUCT_BUNDLE_IDENTIFIER` (`com.nowarmoreweed.pomdaisy`) auf eine eigene ändern.
   3. In App Store Connect eine App mit genau dieser Bundle-ID anlegen.
   4. In App Store Connect unter **Benutzer und Zugriff > Integrationen** einen API-Schlüssel anlegen (Rolle
      „App-Manager“) und die `.p8`-Datei herunterladen.
   5. Im Fork unter **Settings > Secrets and variables > Actions** vier Secrets anlegen:
      `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8` (der Inhalt der `.p8`-Datei) und `APPLE_TEAM_ID`.
   6. Auf den Branch `ios` pushen. Der Workflow `iOS` (`.github/workflows/ios.yml`) baut die App und lädt sie in
      TestFlight hoch. Der erste Lauf dauert etwa 45 Minuten, spätere sind schneller.
   Ohne die Secrets läuft der Workflow trotzdem, baut aber nur zur Kontrolle und lädt nichts hoch.
3. **Sideloaden (SideStore, AltStore).** Dafür braucht man eine `.ipa`-Datei. Die erzeugt der Workflow im Moment
   noch nicht.

## Für Entwickler

- `ios/App`: die iOS-App (Swift, XcodeGen-Projekt `project.yml`).
- `ios/core`, `ios/ds`, `ios/melonds`: die Kerne für die iOS-Oberfläche (CMake, ohne JIT).
- `ios/native`: der native Teil der Android-App für iOS. Der JNI-Code der App läuft unverändert über ein eigenes,
  kleines JNI (`include/jni.h`, `pd_jni.c`).
- `ios/compose`: die Android-App für iOS (Compose Multiplatform). `appui` übersetzt die Quellen aus `app/src/main`
  unverändert; was es auf iOS nicht gibt (Android-Views, OpenGL, Ton, Dateien, Threads, `java.*`), bilden die Ersatzteile
  in `src/shims` mit iOS-Mitteln nach.
- Prüfen ohne Mac: `cd ios/compose && ../../gradlew :appui:compileKotlinIosArm64` kompiliert die ganze App auch
  unter Linux. Linken und den Simulator gibt es nur auf macOS, also im CI.
- **Simulator-Test im CI:** Bei jedem Push startet der Job `simulator` die App auf einem iPhone-Simulator, öffnet die
  Android-Ansicht und zwei eigene Testprogramme (`linux/make_diagnostic.py`, kein Spielcode). Screenshots, Konsole und
  Absturzberichte landen im Artefakt `simulator` des Laufs.
- **TestFlight-Feedback:** Der Workflow `iOS TestFlight feedback` holt Absturzberichte und Feedback aus App Store
  Connect und lädt sie **verschlüsselt** hoch (das Repo ist öffentlich). Lesen kann sie nur, wer den privaten
  Schlüssel zu `ios/feedback_pub.pem` hat. In einem Fork einen eigenen Schlüssel erzeugen und `feedback_pub.pem`
  ersetzen.

Rechtliches, Lizenzen und Danksagungen gelten wie für PokeDaisy (siehe das [Haupt-README](../README.md) und `NOTICE`).
