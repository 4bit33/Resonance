---
name: Resonance Audio
colors:
  surface: '#121316'
  surface-dim: '#121316'
  surface-bright: '#38393c'
  surface-container-lowest: '#0d0e11'
  surface-container-low: '#1b1b1f'
  surface-container: '#1A1C20'
  surface-container-high: '#22252B'
  surface-container-highest: '#2C3038'
  on-surface: '#e3e2e6'
  on-surface-variant: '#dfc0b3'
  inverse-surface: '#e3e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#a78b7f'
  outline-variant: '#584238'
  surface-tint: '#ffb693'
  primary: '#ffb693'
  on-primary: '#561f00'
  primary-container: '#ff7a30'
  on-primary-container: '#622400'
  inverse-primary: '#a04100'
  secondary: '#45f3d5'
  on-secondary: '#00382f'
  secondary-container: '#00d6ba'
  on-secondary-container: '#00574b'
  tertiary: '#fabc4d'
  on-tertiary: '#432c00'
  tertiary-container: '#d0962a'
  on-tertiary-container: '#4c3300'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdbcc'
  primary-fixed-dim: '#ffb693'
  on-primary-fixed: '#351000'
  on-primary-fixed-variant: '#7a2f00'
  secondary-fixed: '#51fbde'
  secondary-fixed-dim: '#1fdec2'
  on-secondary-fixed: '#00201b'
  on-secondary-fixed-variant: '#005045'
  tertiary-fixed: '#ffdead'
  tertiary-fixed-dim: '#fabc4d'
  on-tertiary-fixed: '#281900'
  on-tertiary-fixed-variant: '#604100'
  background: '#121316'
  on-background: '#e3e2e6'
  surface-variant: '#343538'
  surface-base: '#121316'
  outline-subtle: '#2B2E37'
  outline-strong: '#3F4450'
  on-surface-primary: '#F2F3F5'
  on-surface-secondary: '#9CA3AF'
  on-surface-muted: '#606775'
  accent-copper-glow: '#FF8A3D'
  accent-copper-dim: '#4A2209'
  audio-cyan: '#2EE5C8'
  audio-cyan-dim: '#063B34'
  status-error: '#FF5C5C'
  status-error-container: '#3E1616'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.03em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  mono-metric:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  space-xxs: 2px
  space-xs: 4px
  space-sm: 8px
  space-md: 12px
  space-lg: 16px
  space-xl: 20px
  space-2xl: 24px
  space-3xl: 32px
  space-4xl: 40px
  space-5xl: 48px
  space-6xl: 64px
  touch-target-min: 48px
  gutter-screen: 16px
  gutter-sheet: 20px
  mini-player-height: 64px
  bottom-nav-height: 68px
---

## Brand & Style

The design system embodies an uncompromising, tactile, and ultra-responsive audio utility built specifically for local Android storage. The visual ethos rejects the ephemeral cloud model—there are no algorithmic recommendations, intrusive banners, or social hooks. Instead, the interface draws heavily from high-end, machined hi-fi studio equipment combined with pure, utilitarian Android Material 3 ergonomics.

### Personality & Tone
- **Precision & Utility:** Every pixel, tap target, and divider serves an operational purpose. Nothing is ornamental.
- **Calm & Focused:** Deep charcoal and slate tones diminish optical fatigue in low-light environments, giving full visual authority to high-fidelity album art.
- **Mechanical Authority:** Controls feel immediate, reliable, and crisp. Interactions mimic the tactile confidence of heavy anodized aluminum switches and precise optical encoders.
- **Privacy & Permanence:** The interface honors offline ownership. It treats personal music libraries with the permanence of physical media.

### Visual Style
A dark-first, low-elevation aesthetic relying on layered tonal surfaces rather than heavy blur shaders or synthetic drop shadows. High-contrast typography paired with an electric warm neon copper accent establishes immediate scanning hierarchy, ensuring effortless one-handed control during movement or listening sessions.

## Colors

The palette is engineered around dark OLED and LCD performance, utilizing deep charcoal foundations (`#121316`) instead of stark pitch black to preserve legibility, depth separation, and prevent smearing on mobile displays.

### Role Mapping
- **Primary (`#FF7A30`):** Electric warm neon copper. Used selectively for active playback states, primary scrubbing heads, key CTA toggles, and the active indicator of the mini player.
- **Secondary (`#2EE5C8`):** Audio cyan. Reserved strictly for audio format badges (FLAC, DSD, 24-bit Hi-Res), active equalizer peaks, and lossless audio signal paths.
- **Surface Hierarchy:**
  - `surface-base` (`#121316`): The canvas behind lists, settings, and main screen scaffolds.
  - `surface-container` (`#1A1C20`): Cards, bottom navigation dock, top application bars, and table headers.
  - `surface-container-high` (`#22252B`): Persistent mini-player, modal sheets, popup menus, and elevated interactive cards.
  - `surface-container-highest` (`#2C3038`): Selected row fills, pressed states, and segmented control chips.
- **Text & Contrast:**
  - `on-surface-primary` (`#F2F3F5`): Primary titles, track names, and values; meets WCAG AAA against all surfaces.
  - `on-surface-secondary` (`#9CA3AF`): Artist names, timestamps, and secondary navigation cues.
  - `on-surface-muted` (`#606775`): Inactive icons, subtle track numbers, and divider accents.

### Dynamic Theming Guidelines
Artwork-derived palette generation is supported via Android Material You extraction, but must be constrained: only the primary accent may adapt, clamped strictly to a minimum contrast ratio of 4.5:1 against `surface-container` (`#1A1C20`). Backgrounds and surfaces remain anchored to the calibrated slate hierarchy to prevent muddy or low-contrast UI shifts.

## Typography

Typography is systematic, legible, and unpretentious, utilizing `Inter` across all structural tiers. Type scales favor tabular stability and immediate scannability over expressive flourish.

### Hierarchy & Application
- **Display & Large Titles (`display-lg`, `display-lg-mobile`):** Used for artist headers and large hero stats. Tight tracking (`-0.02em`) creates a solid industrial punch without sacrificing screen space.
- **Player Titles & Screen Headers (`headline-lg`, `headline-md`):** Reserved for the Now Playing track title and modal bottom sheet headers. Clamped to two lines maximum with smart truncation (`TextOverflow.Ellipsis`).
- **Body & Secondary Information (`body-md`, `body-sm`):** Handles track metadata, album subtitles, and preference summaries. Track duration and technical details employ tabular numbers (`fontFeatureSettings = "tnum"`) to eliminate jitter during real-time scrubbing.
- **Audio Metrics (`mono-metric`):** Specialized small label scale using uppercase styling and letter spacing (`0.05em`) for sample rates (e.g., `96 kHz / 24-bit FLAC`).

## Layout & Spacing

A hard 4px/8px incremental grid underpins all positioning, padding, and alignments, mapping cleanly to Jetpack Compose `dp` units.

### Structural Architecture
- **Screen Margins:** Fixed 16dp horizontal padding on mobile form factors; expanding to 24dp margins on foldable/tablet outer gutters.
- **List Metrics:**
  - Standard song row height: 64dp (inclusive of 8dp top/bottom inner vertical padding).
  - Compact song row height: 48dp (queue inspect mode).
  - Album grid layout: 2-column on standard portrait mobile (<600dp), 3-column on landscape or small tablets (600dp–840dp), 4-column on full tablets (>840dp).
- **Persistent Overlay Anchors:**
  - The Bottom Navigation bar maintains a rigid height of 68dp.
  - The Mini Player sits docked immediately above the bottom navigation bar with a height of 64dp, spanning full screen width with 8dp inset horizontal margins (floating dock card).
  - Content lists enforce an explicit bottom `WindowInsets` consumer of `68dp + 64dp + 16dp = 148dp` to avoid clipping under persistent audio controls.

## Elevation & Depth

Visual hierarchy does not rely on diffused drop shadows, which look artificial and tax render pipelines. Instead, depth is achieved purely through **tonal container tiers** and **precision low-contrast ghost borders**.

### Layering Blueprint
- **Base Level 0 (`surface-base` / `#121316`):** The master canvas. Zero outline, zero shadow.
- **Level 1 (`surface-container` / `#1A1C20`):** List card groupings, top search bar, and inactive album artwork containers. Outlined by 1dp solid `outline-subtle` (`#2B2E37`).
- **Level 2 (`surface-container-high` / `#22252B`):** Mini player, dialog boxes, context dropdown menus, and standard bottom sheets. Elevated with a 1dp top highlight border (`#3F4450`) to provide crisp edge definition against darker underlying views.
- **Level 3 (`surface-container-highest` / `#2C3038`):** Pressed list items, active chip selectors, and modal overlays.
- **Now Playing Sheet:** A fully opaque sheet rising from the bottom with a 1dp hairline border (`outline-subtle`) around the header edge, completely isolating playback controls from background list noise.

## Shapes

Shapes follow a strict functional hierarchy. Curvature is tied directly to physical scale and interaction category.

### Radius Distribution
- **Controls & Small Components (8dp / `RoundedCornerShape(8.dp)`):** Song row artwork thumbnails, filter chips, audio badge pills, input text fields, and list hover states. Maintains an organized, compact rhythm for high-density lists.
- **Cards & Primary Modules (16dp / `RoundedCornerShape(16.dp)`):** Grid album art tiles, artist profile cards, settings cluster groups, and the Mini Player floating container.
- **Modal Sheets & Overlays (24dp / `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`):** Now Playing full-screen expansion sheet, context menu bottom sheets, and confirmation dialogs.
- **Circular Elements (Fully Pill / 999dp):** Play/pause floating buttons, queue scrub head indicators, and track duration timing tags.

## Components

### 1. Navigation & App Bars
- **Bottom Navigation Dock:** 68dp tall, built upon `surface-container` (`#1A1C20`). Hosts exactly 4 destinations: Home, Library, Playlists, and Settings. Icons are 24dp single-stroke outlines (`#9CA3AF`), shifting to solid fill in `primary_color_hex` (`#FF7A30`) with an underlying 4dp indicator dot when active. Search is globally accessible via a persistent icon button in the Top App Bar across all tabs.
- **Top App Bar:** 56dp height, flat zero-elevation surface. Houses screen title (`headline-md`), quick search button, and library multi-select/sort action.

### 2. Playback System
- **Mini Player:**
  - Placed at `bottom = 68dp` (directly over navigation).
  - Dimensions: 64dp height, 16dp rounded corners, background `surface-container-high` (`#22252B`), 1dp border `#3F4450`.
  - Contents: 48dp squared thumbnail (left, 8dp margin), track title + artist column (center-fill), 48dp touch-target play/pause button (right), optional skip button.
  - Linear track progress: A 2dp accent track running seamlessly across the bottom perimeter of the container (`#FF7A30` elapsed, `#2B2E37` total).
  - Gesture: Tap opens full Now Playing; horizontal swipe skips/previouses track.
- **Now Playing View:**
  - Large artwork: 1:1 aspect ratio square container with 16dp corner radius.
  - Scrub Bar: 4dp height track (expands to 6dp on touch), `#FF7A30` elapsed bar with an 14dp circular thumb (`#F2F3F5`). Accompanying `mono-metric` timestamps flanking left and right.
  - Primary Playback Row: Symmetrical 5-button layout. Center Play/Pause button is 64dp circular container filled with `#FF7A30` and `on-primary` (`#121316`) icon. Previous and Next buttons are 48dp circular ghost buttons (`#F2F3F5`). Outer buttons: Shuffle and Repeat toggles that illuminate from `#606775` to `#FF7A30` when active.
  - Technical Pill: Compact chip centered under artwork displaying real-time audio stats: e.g., `FLAC • 96kHz / 24-bit` colored with `audio-cyan` (`#2EE5C8`).

### 3. Song Rows & Lists
- **Standard Song Row:**
  - Total row height: 64dp; hit area: 100% width x 64dp.
  - Left: 48dp artwork with 8dp radius. If artwork is absent, render a subtle geometric vinyl groove icon in `#2C3038` against `surface-container`.
  - Center: Two-tier vertical text column (Track title in `title-md` `#F2F3F5`; Artist and Album in `body-sm` `#9CA3AF`).
  - Right: Duration (`mono-metric` `#9CA3AF`) alongside a 48dp hit-target overflow vertical dots menu (`#9CA3AF`).
- **Active Playing Row:**
  - Row background fills with an 8% alpha tint of `#FF7A30`. Track title changes color to `#FF7A30`. Replace track number or artwork index with a live 3-bar animated equalizer graphic (`#FF7A30`).

### 4. Interactive Elements & Inputs
- **Buttons & Chips:**
  - Primary Action: 48dp height, 8dp radius, solid `#FF7A30` background, `#121316` bold text.
  - Filter Chips: 32dp height, 8dp radius, `surface-container-highest` background, 1dp border `outline-subtle`. Selected state fills with `#FF7A30` at 15% opacity with an active `#FF7A30` border.
- **Search Input:**
  - 48dp height, 8dp radius, filled with `surface-container-high` (`#22252B`), placeholder `#606775`, active text `#F2F3F5`. Clean trailing "Clear" button with guaranteed 48dp touch area.

### 5. Artwork Placeholders & Empty States
- Missing art employs no generic question marks or cartoon illustrations. Instead, it displays a precision dark slate box (`#1A1C20`) featuring an abstract geometric audio waveform etched in `#2C3038`.
- Library empty states provide an honest, technical explanation ("No audio files found in indexed folders") accompanied by a single primary CTA: "Select Storage Folders" that triggers the Android SAF (Storage Access Framework) directory picker.