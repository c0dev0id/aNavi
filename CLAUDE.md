# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

aNavi is a minimal, offline-first motorcycle navigation app for Android 14+ (`minSdk 34`), targeting low-budget devices. All map, POI, and routing data is available offline — no network calls to external services.

Design goals: fast, slim, simple. Prefer performance over convenience.

## Build & Development

No Gradle wrapper committed — use `gradle wrapper` to generate it, or use a system `gradle` install.

```sh
gradle assembleDebug          # Build debug APK
gradle test                   # Unit tests
gradle test --tests "dev.anavi.SomeTest"  # Single test class
gradle lint                   # Android lint
gradle installDebug           # Install on connected device
gradle assembleRelease        # Signed release (needs keystore, see CI)
```

## Stack

| Layer | Choice | Why |
|---|---|---|
| Map rendering | MapLibre Native GL | GPU-accelerated, offline vector tiles, keeps CPU free |
| UI | Views + custom HUD overlay | Zero abstraction tax; no Compose overhead |
| Theme | `Theme.DeviceDefault.NoActionBar` | Platform theme, no AppCompat needed at API 34+ |
| DI | Manual construction | ~5 services total; no framework warranted |
| POI search (planned) | SQLite FTS5 | Built into Android, sub-ms prefix search |
| Routing (planned) | GraphHopper embedded | On-device motorcycle profile support |

## Architecture

Single-Activity app. `MainActivity` owns the MapView lifecycle and GPS updates. `ANaviApp` initializes MapLibre.

Key constraints:
- **No network dependencies** at runtime
- **No AndroidX in app code** — API 34+ platform APIs suffice; MapLibre pulls AndroidX transitively but we don't use it directly
- **Minimal dependencies** — prefer Android SDK built-ins over third-party libraries
- **Low-end device targets** — avoid memory-heavy operations; profile before adding features

## CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`):
- **build** job: lint, test, assembleDebug — runs on all pushes and PRs
- **release** job: signed APK → GitHub pre-release (draft=false) — runs on main only

Required GitHub Secrets:
- `SIGNING_KEYSTORE_BASE64` — base64-encoded release keystore
- `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`
- `MAPTILER_KEY` — MapTiler API key (map tiles/styles)
- `GOOGLE_STT_KEY` — Google Speech-to-Text key (voice commands)

API keys are injected via `BuildConfig.MAPTILER_KEY` / `BuildConfig.GOOGLE_STT_KEY`. For local dev, set them as environment variables or in `gradle.properties`.
