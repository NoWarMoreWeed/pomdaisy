# PomDaisy (iOS): was da ist, was fehlt

Vergleich mit der Android-App (PokeDaisy). Grundlage: die Projektnotizen (`CLAUDE.md`) und der Stand des Branches `ios`.

- ✅ vorhanden
- 🟡 teilweise / ungetestet
- ❌ fehlt

Was im Kern steckt (`libretro/`, `linux/`, `3ds/core/`), hat PomDaisy automatisch. Alles, was in der Android-App in Kotlin
geschrieben ist, muss für iOS neu gebaut werden.

## Grundfunktionen

| Funktion | Status | Anmerkung |
|---|---|---|
| GBA, Game Boy, Game Boy Color | ✅ | ein mGBA-Kern, ohne JIT |
| Nintendo DS | 🟡 | DSperate-Kern ohne JIT (Interpreter), auf dem iPhone 14 Pro Max bei rund 40 % Auslastung. Beide Bildschirme: hochkant übereinander, quer der obere groß und der Touchscreen daneben. Kein echtes Widescreen (EXPAND gibt es nur im melonDS-Kern der Android-App) |
| Spieleliste, Import (Dateien-App, Teilen, +) | ✅ | |
| ZIP / 7z-Archive | ❌ | |
| Spielnamen statt Dateinamen (Hash-Erkennung) | ❌ | aktuell nur der Dateiname ohne Klammern |
| Cover-Bilder (RetroAchievements, SteamGridDB, ScreenScraper) | ❌ | |
| Favoriten, Sortierung, NEU-Markierung, ausblenden | ❌ | |
| Spielstand schreiben, Sicherung vor jedem Start (10 Kopien) | ✅ | |
| Sicherungen wiederherstellen, Spielstand laden (LOAD SAVE) | ❌ | Dateien liegen in `saves/backups`, aber ohne Oberfläche |
| Zwischenstand beim Verlassen, Fortsetzen beim Start | ✅ | |
| Speicherplätze | 🟡 | 3 Plätze ohne Vorschaubild |
| Hochformat und Querformat, iPhone und iPad | 🟡 | ungetestet auf dem iPad |
| Touch-Steuerung | 🟡 | feste Anordnung; kein Editor, keine Größen, Looks, Farben |
| Delta-Skins | ❌ | |
| Controller (MFi, Xbox, PlayStation, Switch) | 🟡 | feste Belegung, ungetestet |
| Tasten neu belegen, Hotkeys, Turbo A / B | ❌ | |
| Einstellungen der App | ❌ | nur die Einstellungen im Begleiter (Kern) |
| 9 Sprachen | ❌ | App-Oberfläche nur Deutsch |
| Hilfe / Handbuch in der App | ❌ | |

## Begleiter, HUD und Schnellmenü (aus dem Kern)

| Funktion | Status | Anmerkung |
|---|---|---|
| Begleiter: Team, Beutel, Karte, Guide, Pokédex, Kampf-Infos, Trainerkarte | 🟡 | Umfang der Linux-/3DS-Version, voll vor allem für FireRed / LeafGreen / Emerald |
| Begleiter für viele ROM-Hacks und Sprachversionen | 🟡 | nur, was der Kern kennt; die Android-App kennt deutlich mehr |
| Begleiter per Touch | 🟡 | Fix im Test |
| HUD über dem Spiel | ✅ | |
| Schnellmenü: PC, FLY, REPEL, BIKE, WILD | 🟡 | Touch im Test |
| Freies Fliegen, Panorama-Flug | 🟡 | im Kern vorhanden, ungetestet |
| EXPAND (Widescreen), ZOOM OUT | 🟡 | Fix fürs Drehen im Test |
| DS-Begleiter (Platin, HeartGold / SoulSilver, Schwarz / Weiß und Nachfolger) | 🟡 | aus dem DS-Kern der Linux-Version: BEGLEITER tippen, halten = Schnellmenü; ungetestet |
| Begleiter und Spiel gleichzeitig (iPad, faltbares iPhone, Fernseher) | ❌ | Kern liefert nur ein Bild; braucht Erweiterung wie bei der 3DS-Version |

## Spielgefühl

| Funktion | Status | Anmerkung |
|---|---|---|
| Vorspulen | 🟡 | ohne Ton |
| STEADY-Musik beim Vorspulen | ❌ | zweiter Emulator, Lied-Erkennung, Schleifen, Aufnahmen |
| Zurückspulen (REWIND) | ❌ | |
| Spieltempo (0,25x bis 4x), Tempo pro Szene, SMART-Vorspulen | ❌ | |
| Auto-Run, Rennen in Gebäuden | ❌ | |
| Sofortiger Text, mehrfach nutzbare TMs | ❌ | |
| Trade-Entwicklungen | ❌ | |
| Tipps zu Funktionen | ❌ | |

## Kampf

| Funktion | Status | Anmerkung |
|---|---|---|
| Kampf-Buttons (FIGHT / POKéMON / RUN), Ein-Tipp-Attacken | ❌ | |
| Automatischer Tausch, Tausch-Vorschläge | ❌ | |
| Attacken-Tipps, IVs / EVs, Gegner-Details | 🟡 | nur, was der Begleiter des Kerns zeigt |
| Kampfstil GEN 4 / GEN 5 | ❌ | |

## Makeover

| Funktion | Status | Anmerkung |
|---|---|---|
| Anderer Spielerheld (PLAYER LOOK), Rivale, Textbox-Look | ❌ | |
| Pokémon läuft hinterher (FOLLOWER) | ❌ | |
| Tag und Nacht | ❌ | |
| Eigene Fahrzeuge (RIDE ON, FLY ON) | ❌ | |

## Werkzeuge

| Funktion | Status | Anmerkung |
|---|---|---|
| ROM patchen (BPS) | ❌ | |
| Spielstand-Editor, Boxen verwalten | ❌ | |
| Tausch zwischen zwei Spielständen | ❌ | |
| Link-Play (2 bis 4 Spiele) | ❌ | |
| Koop über Netzwerk | ❌ | |
| Cheats (GameShark, Action Replay, CodeBreaker) | ❌ | |
| RetroAchievements | ❌ | |

## Optik und Ton

| Funktion | Status | Anmerkung |
|---|---|---|
| Shader (LCD, CRT, Scanlines, GBA-Farben, Glättung) | ❌ | |
| Rahmen / Overlays | ❌ | |
| Themes, Dunkelmodus der App | ❌ | App ist immer dunkel |
| Statusleiste, FPS-Anzeige | 🟡 | Live-Anzeige in der Ecke (Auslastung Ø / max, fps), im Pause-Menü abschaltbar; keine Statusleiste |
| Tasten-Klick, Fanfare bei Erfolgen | ❌ | |

## Nächste Schritte (Vorschlag)

1. Touch hochkant und EXPAND beim Drehen bestätigen
2. STEADY-Musik beim Vorspulen (richtig, nicht beschleunigt)
3. Zurückspulen
4. Kampf-Buttons
5. Begleiter und Spiel gleichzeitig (iPad / faltbares iPhone / Fernseher)
