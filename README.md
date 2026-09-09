<p align="center">
  <img src="https://img.shields.io/badge/Pulse%20Player-v1.0.2-7C3AED?style=for-the-badge" alt="version"/>
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="kotlin"/>
  <img src="https://img.shields.io/badge/License-MIT-22C55E?style=for-the-badge" alt="license"/>
  <img src="https://img.shields.io/badge/Built%20by-Glass%20Line-06B6D4?style=for-the-badge" alt="glass line"/>
  <img src="https://img.shields.io/badge/Lab-Velocity-F59E0B?style=for-the-badge" alt="velocity lab"/>
</p>

# Pulse Player

Native Android music player built with **Kotlin** and **Jetpack Compose**.

Immersive glass UI · local library · background playback · lyrics · metadata enrichment · in-app updates.

**Velocity Lab** · **Built by Glass Line**

---

## Features

| Area | Details |
|------|---------|
| **Playback** | MediaPlayer + MediaSession, notification controls, audio focus |
| **Library** | MediaStore scan, favorites, Room cache |
| **Now Playing** | Full-screen player, seek, mini-player, back-to-close |
| **Lyrics** | LRCLIB fetch, plain + synced LRC with line tracking |
| **Metadata** | Embedded tags + MusicBrainz enrichment for local files |
| **Updates** | GitHub Releases check, download with % / speed / pause / resume |
| **Themes** | Pulse Realms, AMOLED mode, performance profiles |

---

## Requirements

- Android 5.0+ (API 21), target API 34
- Storage / music permission to scan the device library

---

## Build

```bash
git clone https://github.com/benjaminchume-droid/Pulse-player-.git
cd Pulse-player-/android
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release (needs keystore)
```

Release signing uses env vars (also used in CI):

- `SIGNING_KEY_STORE_BASE64`
- `PULSE_KEY_ALIAS`
- `PULSE_KEY_PASSWORD`
- `PULSE_STORE_PASSWORD`

---

## Release (CI)

Push a version tag to build, sign, and publish APK + AAB:

```bash
git tag v1.0.2
git push origin v1.0.2
```

Workflow: `.github/workflows/production_release.yml`

---

## Architecture

```
android/app/src/main/kotlin/com/pulseplayer/music/
├── MainActivity.kt
├── data/          # Room Song, Playlist, Dao
├── service/       # PlaybackService + MediaSession
├── viewmodel/     # PlaybackViewModel
├── lyrics/        # LRCLIB client + LRC parser
├── metadata/      # MediaMetadataRetriever + MusicBrainz
├── update/        # GitHub Releases updater
└── ui/            # Compose screens & components
```

---

## License

MIT © 2026 Velocity Lab / Glass Line

---

<p align="center">
  <b>Velocity Lab</b> · Built by <b>Glass Line</b>
</p>
