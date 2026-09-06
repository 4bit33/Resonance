# Resonance — Architecture (Phase 1 foundation)

Local-first, offline-first music player. No INTERNET permission by design.
Single `:app` Gradle module with strict package layers (solo-dev friendly;
split into Gradle modules only when build times or reuse demand it).

## Dependency direction

UI (Compose) -> ViewModel -> UseCase -> Repository -> Data Source (Room / MediaStore)

- UI never touches ExoPlayer, MediaController, Room entities, or DAOs.
- Domain (`core/model`, `domain/*`) is pure Kotlin: no Android imports
  (except DataStore/DTO boundaries), fully unit-testable.
- `core/common.Result` + `AppError` is the only failure channel to the UI.

## Ownership

- Playback state: exactly ONE `StateFlow<PlaybackSnapshot>`, owned by
  `PlaybackController`. Phase 1 ships `UnimplementedPlaybackController`
  (idle + `FeatureUnavailable`, never fake progress). Phase 2 implements the
  same interface with Media3 (ExoPlayer + MediaSession + foreground service).
- Database: single `ResonanceDatabase` (Room v1). Entities map to domain via
  `SongMapper` — entities never leak to UI. Paged/Flow reads only.
- Settings: DataStore (`UserPreferencesRepository`), never the database.
- Permissions: all SDK branches isolated in `core.permissions.MusicPermissions`.

## Decisions (ADR)

- ADR-001 versions: AGP 8.10.1 / Kotlin 2.0.21 / Gradle 8.11.1 / compileSdk 36 + targetSdk 35 (36 required by Media3 1.10.1; target stays 35 deliberately)
  — proven working on this host (reference project). Room 2.8.4 (stable, KSP);
  Room 3.x rejected (breaking `room3` rewrite, unnecessary). Media3 1.10.1
  (stable) on classpath for Phase 2. Navigation-Compose 2.7.7 (conservative).
- ADR-002 minSdk 26: notification channels + Media3 baseline; API 29/33
  branches guarded in one place. No per-version UI duplicates.
- ADR-003 playback boundary: `core.playback.PlaybackController` interface.
- ADR-004 entities never reach UI (`SongMapper` is the single bridge).
- ADR-005 song identity = stable id (MediaStore audio id at import); path/URI
  indexed, never sole identity.
- ADR-006 manual `AppContainer` DI; no Hilt/Koin (build cost, coupling).
- ADR-007 Smart Mix stays interfaces-only (`domain.mix`) with local-only,
  testable constraints; algorithm in a later phase.

## What is real in Phase 1

Room schema + DAOs, RoomMusicRepository reads, DataStore theme setting,
Library/Search/Details screens over real (initially empty) data, error/empty
states, adaptive bar-vs-rail navigation, JVM unit tests.

## Host note (Windows, Cyrillic username)

Gradle test workers fail when GRADLE_USER_HOME contains non-ASCII characters.
Fix applied on this machine: user env `GRADLE_USER_HOME=F:\Gradle\home`
(ASCII path). Restart shells/Android Studio to pick it up.

## Next (Phase 2)

MediaStore scanner + tag extractor, Media3 service + session, real
`PlaybackController`, notification / headset / audio-focus handling.
