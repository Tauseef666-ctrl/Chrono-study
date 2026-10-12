# CHRONO STUDY

**Plan • Focus • Achieve**

A premium native Android digital clock, study screensaver, Pomodoro / custom focus timer and
diploma study companion.

- **Platform:** Native Android (Kotlin + Jetpack Compose + Material 3)
- **Min SDK:** 26 (Android 8.0) · **Target/Compile SDK:** 35
- **Local-first:** Room + DataStore, works fully offline

---

## Features

- Full-screen landscape digital clock / screensaver with battery, date, subject, chapter and
  live focus countdown.
- Accurate subject- and chapter-wise study-time tracking derived from persisted monotonic
  timestamps (survives pause, rotation, process death, reboot and clock changes).
- Focus timer with an explicit, testable state machine (Idle / Running / Paused / Completed /
  Cancelled) plus Finish Early and Cancel policies.
- Break timer (short / long / custom) tracked separately from focused study.
- Reliable completion alerts: sound + vibration / sound only / vibration only / silent, with
  bundled tones, preview and test.
- Study library (subjects + chapters), daily/weekly planner, dashboard, analytics and
  chapter-completion tracking.
- Seven theme presets, accent colors, accessibility and responsive portrait/landscape layouts.
- Validated JSON backup / restore and CSV export via the system document picker.

## Building

```bash
./gradlew assembleDebug      # debug APK
./gradlew testDebugUnitTest  # unit tests
./gradlew lintDebug          # Android lint
./gradlew assembleRelease    # release APK
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Release signing

The release build always produces an installable APK. If no signing configuration is present
it falls back to the debug key so CI can publish an artifact. To produce a properly signed
release, create a `keystore.properties` file in the repository root (never committed):

```properties
storeFile=/absolute/path/to/release.keystore
storePassword=********
keyAlias=chrono
keyPassword=********
```

For GitHub Actions, add these repository secrets and the release workflow will sign
automatically:

| Secret | Purpose |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of your `.keystore` (`base64 -w0 release.keystore`) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

Without those secrets the release workflow publishes an APK signed with the debug key.

## CI / Releases

- `.github/workflows/android-ci.yml` — tests, lint and a debug APK artifact on every push/PR.
- `.github/workflows/release.yml` — builds a release APK and attaches it to a GitHub Release
  when a `v*` tag is pushed (or via manual dispatch).

## Study-time accuracy rules

- Only eligible active focus time is counted; pauses and breaks are excluded.
- Planned duration is **never** used as actual study duration.
- Each eligible interval belongs to exactly one subject/chapter.
- Totals are recomputed from saved records; historical attribution is preserved even after a
  subject is renamed or archived.
- Completion handlers are idempotent — no duplicate records or alerts.
