# Crate — Architecture (Phase 3: library ingestion pipeline, music sources)

Local-first, offline-first music player. No INTERNET permission by design
(verified in the merged manifest), and no storage/audio permission either.
Single `:app` Gradle module with strict package layers (solo-dev friendly;
split into Gradle modules only when build times or reuse demand it).

## Dependency direction

UI (Compose) -> ViewModel -> UseCase -> Repository -> Data Source (Room / Storage Access Framework)

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
- Library ingestion: `SafLibraryScanner` (app scope, single-flight,
  cancellable) owns discovery -> extract -> normalize -> artwork ->
  reconcile -> persist. Composables never touch ContentResolver.
- Music sources: `RoomSourceRepository` is the ONLY place that takes or
  releases a persisted read grant. The system pickers live once in the app
  shell (`LocalMusicActions`); screens only trigger them.
- Queue: runtime-only `QueueBookkeeper`. Never touches saved data.
- Database: single `ResonanceDatabase` (Room v3). Entities map via
  `SongMapper`. Songs belong to a source (FK, ON DELETE CASCADE: removing a
  source drops its songs). Playlists/favorites/history hold NO foreign keys
  to songs, so removing songs never cascades user data.
- Settings + restore + library prefs: DataStore.
- Access: no storage/audio permission. Music is what the user picked
  (ADR-010); playback keeps its foreground-service/notification permissions.

## Ingestion pipeline

User-added sources (folder = TREE via OpenDocumentTree, song = FILE via OpenMultipleDocuments)
  -> SafAudioDataSource (DocumentsContract, ONE child query per directory;
     per-source COMPLETE / INCOMPLETE / UNAVAILABLE)
  -> AudioCandidate (pure row mapping, stable song id)
  -> AndroidMetadataExtractor (MediaMetadataRetriever, best-effort nulls)
  -> MetadataNormalizer (tags > filename; pure, tested)
  -> RetrieverArtworkExtractor + ArtworkStore (SHA-256 file cache, dedup)
  -> SourceReconciler (pure, source-scoped NEW/MODIFIED/UNCHANGED/DELETED diff)
  -> Room batches (100 upserts, 500-id delete chunks, stats preserved)
  -> Repository Flows -> ViewModels -> Compose (Coil lazy artwork)

- Identity: `Song.id` = 63-bit hash of provider authority + documentId
  (ADR-010); change detection by (dateModifiedSec, sizeBytes, sourceId). A
  moved or renamed file is a new song (documented tradeoff; favorites and
  playlist entries are FK-less and keyed by id).
- Deletion safety: a stored song is deleted only when its source was listed
  COMPLETE and no longer contains it. A source that is unavailable (grant
  lost, card unmounted) or only partly listed never deletes anything.
- Metadata priority: embedded tags > filename fallback > Unknown-* display
  constants (Unicode preserved, no case folding, no transliteration, source
  files never modified). Folder listings skip dot-files (AppleDouble,
  `.trashed-*`); a file the user picked explicitly is trusted as audio.
- Track/disc parse "1", "01", "1/12"; year accepts 1000..2999 from
  YYYY[-MM-DD]; duration comes from tags, else 0 (unknown, never faked);
  missing genre stays null (excluded from genre groups). "Ignore short
  files" is applied after extraction (short files are re-read on every
  refresh; a persisted marker is the upgrade if that ever hurts).
- First-seen date: `dateAdded` = min(now, file mtime), kept across re-imports.
- Compilations: albums group by (title, album-artist); display prefers
  album artist, else single artist, else "Various Artists".
- Album/artist identity is name-normalized (case-insensitive); "Greatest
  Hits" by different album-artists stays separate.
- Artwork: raw embedded bytes (bounds-validated, 5 MB cap) -> SHA-256 file
  in app cacheDir/artwork (dedup across tracks); DB keeps key + file URI
  only. Missing files re-extract on next scan; orphans pruned post-scan.
  Coil loads lazily with fallback; MediaItem carries the file URI to the
  session (never blocks playback startup).
- Scan triggers: right after a source is added or removed, and the
  "Refresh library" actions (Settings row, Home card). Nothing scans on app
  start or resume. Cached Room data renders instantly. Adding or removing a
  source cancels a running scan first (the scanner is single-flight, so a
  scan in progress would swallow the follow-up request).
- Lost access: a source without a live grant is skipped and listed as
  "Access lost"; its songs stay. Re-adding the same folder (idempotent)
  re-takes the grant and repairs it.
- Concurrency: 4-permit semaphore for extraction, IO dispatcher everywhere,
  cooperative cancellation, no GlobalScope, empty-queue/empty-library
  states in UI.

## Decisions (ADR)

- ADR-001 versions: AGP 8.10.1 / Kotlin 2.0.21 / Gradle 8.11.1 / compileSdk 36 + targetSdk 35 (36 required by Media3 1.10.1; target stays 35 deliberately). Room 2.8.4 (KSP); Media3 1.10.1; Coil 3.2.0 (era-matched to Compose 1.8.2, Apache 2.0, file-URI loads only — no network module). No tag-library dependency: platform MMR is sufficient and best-effort.
- ADR-002 minSdk 26: API 29/33 branches guarded in one place.
- ADR-003 playback boundary: `core.playback.PlaybackController`.
- ADR-004 entities never reach UI (`SongMapper` is the single bridge).
- ADR-005 (superseded by ADR-010) song identity = MediaStore audio id at import; path/URI indexed, never sole identity.
- ADR-006 manual `AppContainer` DI; app-lifetime scope for playback + scanner.
- ADR-007 Smart Mix stays interfaces-only; song metadata (genre, counts, history) is scoring-ready, no fake BPM/key.
- ADR-008 stop() = halt + keep queue + reset to head; errors never auto-advance; restore never autoplays.
- ADR-009 schema upgrades are explicit Migrations (v1->v2 additive, keeps all user data; v2->v3 is a deliberate clean start for songs, see ADR-010); destructive fallback applies to downgrades only.
- ADR-010 the library is the music the user adds, not a device scan. Folders (OpenDocumentTree) and single songs (OpenMultipleDocuments) with persisted READ grants; scans run only after add/remove and on "Refresh library"; no READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE. `Song.id` stays a Long (nav arg, Media3 media id, DataStore queue, favorites/playlists untouched) but becomes a stable hash of authority + documentId, pinned by a golden-value test because it is a persisted format. Songs FK to `sources` with CASCADE; source inserts use IGNORE (REPLACE would delete the parent row and cascade every song). v2->v3 drops the old songs and empties playlist items, favorites and history (playlist names are kept); the DDL is copied from the Room-generated code. "Remove" in the UI always means remove from the library: files are never deleted.
  Known limits: Android caps persisted grants (512, 128 before API 30; a picked file costs one) and grants do not survive backup/restore (`allowBackup=true`), so access is derived from `persistedUriPermissions` and re-adding repairs it; Android 11+ refuses the storage root and the Download folder in the folder picker (add a subfolder, or its files via Add songs); `.nomedia` / `IS_MUSIC` are no longer honoured; the same file reached through two providers (picker's Audio tab vs a folder) can appear twice.

## What is real

Real SAF discovery of user-added folders and songs, tag extraction,
normalization, artwork cache, source-scoped incremental reconciliation,
Room v3, Library tabs (Songs/Albums/Artists/Genres/Folders), extended
Search, tap-to-play + play-album into the Phase 2 engine, Now Playing
artwork, Settings music sources / refresh / stats, 108 JVM tests.

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
- Deliberately NOT built (product conflicts): tag
  edit/file delete, DSP/EQ engine/ReplayGain/crossfade/sleep timer/bit-
  perfect claims, account avatar, GPL badge, selectable radii/accents,
  missing-file Locate rows (auto-prune stays), Smart Mix UI.

## Next

Gestures (swipe down to close Now Playing, swipe up / left / right on the
mini player, long-press a song for its menu incl. "Remove from library"),
screen-by-screen Stitch reskin on these components, playlist management
UI, Smart Mix engine feeding generated queues, release minification
(strips unused icons), backup/export.
