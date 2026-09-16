# Crate — Architecture (Phase 3: library ingestion pipeline)

Local-first, offline-first music player. No INTERNET permission by design
(verified in the merged manifest).
Single `:app` Gradle module with strict package layers (solo-dev friendly;
split into Gradle modules only when build times or reuse demand it).

## Dependency direction

UI (Compose) -> ViewModel -> UseCase -> Repository -> Data Source (Room / MediaStore)

- UI never touches ExoPlayer, MediaController, Room entities, DAOs,
  ContentResolver or cursors.
- Domain (`core/model`, `domain/*`) is pure Kotlin, fully unit-tested.
- `core/common.Result` + `AppError` is the only failure channel to the UI.

## Ownership

- Playback state: exactly ONE `StateFlow<PlaybackSnapshot>`, built by
  `RealPlaybackController` from real player values. Notification, lock
  screen, Bluetooth and UI all render MediaSession/UI state derived from it.
- Player/session: `playback.PlaybackService` (MediaSessionService) is the
  ONLY owner of ExoPlayer + MediaSession. One instance per process.
- Library ingestion: `MediaStoreLibraryScanner` (app scope, single-flight,
  cancellable) owns discovery -> extract -> normalize -> artwork ->
  reconcile -> persist. Composables never query MediaStore.
- Queue: runtime-only `QueueBookkeeper`. Never touches saved data.
- Database: single `ResonanceDatabase` (Room v2). Entities map via
  `SongMapper`. Playlists/favorites/history hold NO foreign keys to songs,
  so deleting vanished files never cascades user data.
- Settings + restore + library prefs: DataStore.
- Permissions: SDK branches in `MusicPermissions`; status in centralized
  `AudioPermissionManager`; UI uses one shared gate/launcher.

## Ingestion pipeline (Phase 3)

MediaStore (IS_MUSIC != 0, _ID ASC, API-gated projection)
  -> MediaStoreAudioDataSource (streaming cursor, per-row isolation)
  -> MediaItemCandidate (pure row mapping, content URIs per volume)
  -> AndroidMetadataExtractor (MediaMetadataRetriever, best-effort nulls)
  -> MetadataNormalizer (tags > columns > filename; pure, tested)
  -> RetrieverArtworkExtractor + ArtworkStore (SHA-256 file cache, dedup)
  -> Reconciler (pure NEW/MODIFIED/UNCHANGED/DELETED diff)
  -> Room batches (100 upserts, 500-id delete chunks, stats preserved)
  -> Repository Flows -> ViewModels -> Compose (Coil lazy artwork)

- Identity: Room PK = MediaStore audio id (ADR-005); change detection by
  (mediaStoreId, volumeName, dateModifiedSec, sizeBytes). A deleted +
  re-added file is a new song (documented tradeoff, keeps FK-less history
  stable). Cross-volume id reuse is handled by composite keys everywhere.
- Metadata priority: embedded tags > MediaStore columns > filename
  fallback > Unknown-* display constants (Unicode preserved, no case
  folding, no transliteration, source files never modified).
- Track/disc parse "1", "01", "1/12"; year accepts 1000..2999 from
  YYYY[-MM-DD]; duration prefers MediaStore, then tags, else 0 (unknown,
  never faked); missing genre stays null (excluded from genre groups).
- Compilations: albums group by (title, album-artist); display prefers
  album artist, else single artist, else "Various Artists".
- Album/artist identity is name-normalized (case-insensitive); "Greatest
  Hits" by different album-artists stays separate.
- Artwork: raw embedded bytes (bounds-validated, 5 MB cap) -> SHA-256 file
  in app cacheDir/artwork (dedup across tracks); DB keeps key + file URI
  only. Missing files re-extract on next scan; orphans pruned post-scan.
  Coil loads lazily with fallback; MediaItem carries the file URI to the
  session (never blocks playback startup).
- Scan triggers: app start + Activity resume + permission grant + Settings
  rescan. Cached Room data renders instantly; scans reconcile in
  background. Permission loss stops scanning, keeps cache, never wipes.
- Concurrency: 4-permit semaphore for extraction, IO dispatcher everywhere,
  cooperative cancellation, no GlobalScope, empty-queue/empty-library and
  permission states in UI.

## Decisions (ADR)

- ADR-001 versions: AGP 8.10.1 / Kotlin 2.0.21 / Gradle 8.11.1 / compileSdk 36 + targetSdk 35 (36 required by Media3 1.10.1; target stays 35 deliberately). Room 2.8.4 (KSP); Media3 1.10.1; Coil 3.2.0 (era-matched to Compose 1.8.2, Apache 2.0, file-URI loads only — no network module). No tag-library dependency: platform MMR is sufficient and best-effort.
- ADR-002 minSdk 26: API 29/33 branches guarded in one place.
- ADR-003 playback boundary: `core.playback.PlaybackController`.
- ADR-004 entities never reach UI (`SongMapper` is the single bridge).
- ADR-005 song identity = MediaStore audio id at import; path/URI indexed, never sole identity.
- ADR-006 manual `AppContainer` DI; app-lifetime scope for playback + scanner.
- ADR-007 Smart Mix stays interfaces-only; song metadata (genre, counts, history) is scoring-ready, no fake BPM/key.
- ADR-008 stop() = halt + keep queue + reset to head; errors never auto-advance; restore never autoplays.
- ADR-009 schema upgrades are explicit additive Migrations (v1->v2 keeps all user data); destructive fallback applies to downgrades only.

## What is real in Phase 3

Real MediaStore discovery, tag extraction, normalization, artwork cache,
incremental reconciliation, Room v2, Library tabs (Songs/Albums/Artists/
Genres/Folders), extended Search, tap-to-play + play-album into the Phase 2
engine, Now Playing artwork, Settings rescan/stats/permission, 83 JVM tests.

## Host note (Windows, Cyrillic username)

Gradle test workers fail when GRADLE_USER_HOME contains non-ASCII characters.
Fix applied on this machine: user env `GRADLE_USER_HOME=F:\Gradle\home`
(ASCII path). Restart shells/Android Studio to pick it up.

## UI design system (Phase 3.5, Stitch foundation)

Visual source of truth: `stitch_form_design_generator/` (DESIGN.md + six
screens). Translated to native Compose — never copied HTML/CSS.

- Tokens: `core.ui.theme` — Stitch dark palette (`ResonanceColors`,
  M3-mapped + custom roles), full type scale (`ResonanceTypography`,
  system sans at Stitch metrics until Inter is bundled), 4px spacing
  scale + semantic paddings (`ResonanceSpacing`), functional radii
  8/16/24-top/full (`ResonanceRadii`), fixed metrics — dock 68dp, mini
  player 64dp, 148dp insets (`ResonanceDimensions`). Access via
  `ResonanceTheme.colors/typography/spacing/radii/dimensions`.
- Stitch dark is authoritative; LIGHT maps to a stock-M3 interim scheme
  (Stitch shipped dark tokens only) and dynamic color is intentionally off
  so tokens stay authoritative. ThemeMode persistence unchanged.
- Font decision: Inter NOT bundled (no res/font; runtime fetching would
  violate offline-first). System sans-serif at Stitch metrics. Adding Inter
  OFL files later is a drop-in `FontFamily` swap in one file.
- Icons: material-icons-extended (already a dep) + local vectors
  (EQ state animation, etched-waveform/note artwork fallbacks). EQ motion
  reflects `isPlaying` only — never claimed as analysis.
- Components: `core.ui.components` — top bar, section header, search
  field, chips, segmented, switch, settings rows, primary button, song
  rows (normal/playing/selected/disabled/missing/loading via pure
  `songRowState`), compact rows, album/playlist cards, mini player,
  queue peek, batch bar, sheets, dialogs, empty states, snackbar voice,
  format badges, tabular metrics, 68dp nav dock.
- Navigation: tabs Home/Library/Playlists/Settings; Search/Player/Queue
  are global routes (`tabForRoute`). Mini player lives in the shell above
  the dock (rail on expanded widths), driven by the ONE playback snapshot;
  status/system bars handled at shell level. `tabs` is lazy by design
  (eager outer static-init reading nested objects is a JLS 12.4.2 null
  hazard — caught by unit test).
- Deliberately NOT built (product conflicts): SAF folder management, tag
  edit/file delete, DSP/EQ engine/ReplayGain/crossfade/sleep timer/bit-
  perfect claims, account avatar, GPL badge, selectable radii/accents,
  missing-file Locate rows (auto-prune stays), Smart Mix UI.

## Next

Screen-by-screen Stitch reskin on these components, playlist management
UI, Smart Mix engine feeding generated queues, release minification
(strips unused icons), backup/export.
