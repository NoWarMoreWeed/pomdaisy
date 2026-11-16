<p align="right"><a href="README.md">🇩🇪 Deutsch</a> · <strong>🇬🇧 English</strong></p>

# PomDaisy: PokeDaisy on iPhone and iPad

PomDaisy is the iOS version of PokeDaisy. It lives on this repo's `ios` branch and is an **early experiment**: it runs,
but much of it hasn't been checked on real devices yet. As of November 2026.

## What's there

The app currently has **two interfaces**:

1. **The iOS interface (what opens at launch).** Written in Swift: game list, game screen, touch controls, pause
   menu. It uses PokeDaisy's shared cores:
   - **GBA, Game Boy, Game Boy Color** on mGBA, with the companion, HUD and quick menu from the core
     (what the Linux and 3DS versions have).
   - **Nintendo DS** on two cores, both **without JIT** (iOS allows no JIT in the App Store or TestFlight):
     DSperate (the default; about 40 % load on an iPhone 14 Pro Max) and melonDS with **EXPAND** (the widescreen
     overworld). Switch between them in the pause menu.
   - What works and what's missing in detail: [FEATURES.md](FEATURES.md) (in German).
2. **"Android-Ansicht" (Android view, the button top left).** This is **the real Android app**: the same Kotlin and
   Compose code as on Android, built for iOS with Compose Multiplatform, plus the app's native code (mGBA, melonDS,
   DSperate, EXPAND). The goal is for PomDaisy to look and feel almost exactly like PokeDaisy on Android, and for new
   features of the Android app to come along by themselves.
   - **Status:** the library and the game screen run in the iOS simulator without crashing (test programs for GBA and
     Game Boy). On a real iPhone it can be tried from TestFlight build 32 on.
   - **Not checked yet:** sound, touch input in games, performance, DS games.
   - **Still missing:** co-op and the link cable, RetroAchievements, a file picker for importing (put ROMs in through
     the Files app, see below), DS RESOLUTION (it needs OpenGL ES 3.2, which iOS doesn't have).
   - Later the Android view is meant to replace the iOS interface completely.

Both interfaces use the same folders, so your games and saves show up in both.

## Games and saves

- **No games included.** You need your own ROM files (`.gba`, `.gb`, `.gbc`, `.nds`). No BIOS files needed.
- ROMs go into the Files app under **On My iPhone > PomDaisy > roms**, or through **+** in the iOS interface.
- Saves are in **PomDaisy > saves** (`<game name>.sav`), backups in `saves/backups`. You can put a save from another
  emulator there if it has the same name as the ROM.

## Installing

There's **no public download**. Three ways:

1. **A TestFlight invite.** The owner of this repo can add you as a tester, on request only.
2. **Build it yourself on GitHub (your own TestFlight).** You need a paid Apple Developer account (99 a year), but no
   Mac: GitHub Actions builds it on a macOS machine.
   1. Fork the repo.
   2. In `ios/App/project.yml`, change `PRODUCT_BUNDLE_IDENTIFIER` (`com.nowarmoreweed.pomdaisy`) to your own.
   3. In App Store Connect, create an app with exactly that bundle ID.
   4. In App Store Connect under **Users and Access > Integrations**, create an API key (role "App Manager") and
      download the `.p8` file.
   5. In your fork under **Settings > Secrets and variables > Actions**, add four secrets:
      `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8` (the contents of the `.p8` file) and `APPLE_TEAM_ID`.
   6. Push to the `ios` branch. The `iOS` workflow (`.github/workflows/ios.yml`) builds the app and uploads it to
      TestFlight. The first run takes about 45 minutes, later ones are faster.
   Without the secrets the workflow still runs, but only builds as a check and uploads nothing.
3. **Sideloading (SideStore, AltStore).** That needs an `.ipa` file, which the workflow doesn't make yet.

## For developers

- `ios/App`: the iOS app (Swift, XcodeGen project `project.yml`).
- `ios/core`, `ios/ds`, `ios/melonds`: the cores for the iOS interface (CMake, no JIT).
- `ios/native`: the Android app's native part for iOS. The app's JNI code runs unchanged over a small JNI of our own
  (`include/jni.h`, `pd_jni.c`).
- `ios/compose`: the Android app for iOS (Compose Multiplatform). `appui` builds the sources from `app/src/main`
  unchanged; what iOS doesn't have (Android views, OpenGL, sound, files, threads, `java.*`) is rebuilt with iOS means by
  the stand-ins in `src/shims`.
- Checking without a Mac: `cd ios/compose && ../../gradlew :appui:compileKotlinIosArm64` compiles the whole app on
  Linux too. Linking and the simulator need macOS, so they run in CI.
- **Simulator test in CI:** on every push the `simulator` job starts the app on an iPhone simulator, opens the Android
  view and two test programs of our own (`linux/make_diagnostic.py`, no game code). Screenshots, console output and
  crash reports go into the run's `simulator` artifact.
- **TestFlight feedback:** the `iOS TestFlight feedback` workflow fetches crash reports and feedback from App Store
  Connect and uploads them **encrypted** (the repo is public). Only the holder of the private key for
  `ios/feedback_pub.pem` can read them. In a fork, make your own key and replace `feedback_pub.pem`.

Legal notes, licenses and credits are the same as for PokeDaisy (see the [main README](../README.md) and `NOTICE`).
