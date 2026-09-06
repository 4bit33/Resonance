Design the foundational UI/UX system for a premium Android local music player.

This is the first design phase of a larger application. Do NOT focus on implementing functionality or backend logic. Focus on establishing a consistent visual language, component system, navigation structure, and interaction principles that will be reused across the entire application.

PRODUCT:

A modern Android music player designed primarily for LOCAL music stored on the user's device.

The application is:
- Free
- No subscriptions
- No advertisements
- No mandatory account
- No cloud requirement
- Privacy-focused
- Offline-first
- Fast and lightweight

The product should feel like a serious standalone music application rather than a Spotify clone.

==================================================
DESIGN DIRECTION
==================================================

Create a distinctive visual identity.

The interface should feel:
- Modern
- Premium
- Minimal
- Fast
- Calm
- Technical but approachable
- Music-focused
- Android-native
- Comfortable for long-term daily use

Avoid:
- Generic Spotify imitation
- Excessive cards
- Excessive gradients
- Excessive glassmorphism
- Giant decorative elements
- Tiny controls
- Excessive animations
- Visual clutter

Album artwork should be visually important, but the interface must still look excellent when artwork is missing.

==================================================
THEME
==================================================

Create a first-class dark theme.

Also define:
- Light theme
- System theme

Dark mode should not simply be an inverted light theme.

Use a carefully balanced hierarchy of:
- Background
- Surface
- Elevated surface
- Primary text
- Secondary text
- Disabled text
- Dividers
- Accent
- Destructive actions

Allow the accent color to work consistently throughout the application.

The accent may optionally be derived from album artwork, but this must remain subtle and readable.

==================================================
TYPOGRAPHY
==================================================

Define a complete typography system.

Include:
- Display
- Large title
- Screen title
- Section title
- Body
- Secondary body
- Caption
- Labels
- Buttons
- Player title
- Player artist

Typography must prioritize readability on mobile screens.

==================================================
SPACING
==================================================

Create a consistent spacing scale.

Use the same spacing logic across:
- Lists
- Cards
- Screens
- Bottom sheets
- Player
- Settings
- Navigation

Avoid arbitrary spacing values.

==================================================
COMPONENT SYSTEM
==================================================

Create reusable components for:

Navigation:
- Bottom navigation
- Top app bar
- Back navigation
- Search button
- Overflow menu

Playback:
- Play button
- Pause button
- Previous
- Next
- Shuffle
- Repeat
- Favorite
- Progress bar
- Mini Player
- Player controls
- Queue button

Music:
- Song row
- Compact song row
- Album card
- Artist card
- Playlist card
- Artwork placeholder
- Section header

Interaction:
- Bottom sheet
- Dialog
- Context menu
- Chips
- Segmented controls
- Switches
- Sliders
- Search field
- Filter/sort controls

States:
- Loading
- Empty
- Error
- Disabled
- Selected
- Pressed
- Playing

==================================================
TOUCH TARGETS
==================================================

Design for comfortable one-handed operation.

Interactive elements should have appropriately large touch targets.

Do not sacrifice usability for visual compactness.

Primary playback controls should be particularly easy to reach.

==================================================
BOTTOM NAVIGATION
==================================================

Use four primary destinations:

1. Home
2. Library
3. Playlists
4. Settings

Search should be accessible globally without taking permanent space from the four primary destinations.

A persistent Mini Player should appear above the bottom navigation whenever music is playing.

==================================================
MINI PLAYER
==================================================

Design the persistent Mini Player.

It should contain:
- Album artwork
- Song title
- Artist
- Play/pause
- Optional next action
- Playback progress

Interaction:
- Tap → open Now Playing
- Play/pause → immediately control playback
- Smooth transition into Now Playing

The Mini Player should never feel like a large card occupying excessive screen space.

==================================================
NOW PLAYING FOUNDATION
==================================================

Establish the visual language for the Now Playing screen.

It should feel like the most important screen in the application.

Define:
- Large album artwork
- Track title
- Artist
- Album
- Progress
- Playback controls
- Secondary actions
- Queue access

Controls should be visually hierarchical.

Primary:
- Previous
- Play/Pause
- Next

Secondary:
- Shuffle
- Repeat
- Favorite
- Queue
- More

==================================================
MUSIC LIST DESIGN
==================================================

Create a consistent song row.

A song row should support:
- Artwork
- Title
- Artist
- Album
- Duration
- Playing indicator
- Overflow menu

Define variants for:
- Normal list
- Compact list
- Currently playing
- Selected
- Disabled/missing file

==================================================
ARTWORK
==================================================

Define artwork styles for:
- Songs
- Albums
- Artists
- Playlists
- Now Playing
- Mini Player

Support missing artwork elegantly.

Do not make missing artwork look like an error.

==================================================
SURFACE AND CORNER SYSTEM
==================================================

Define a coherent corner-radius hierarchy.

Use different radii intentionally for:
- Small controls
- List elements
- Cards
- Bottom sheets
- Dialogs
- Large artwork containers

Do not round everything excessively.

==================================================
ICONS
==================================================

Use a consistent icon family.

Icons should be:
- Simple
- Recognizable
- Android-friendly
- Consistent in stroke/weight

Do not use decorative icons when a standard familiar symbol exists.

==================================================
MOTION
==================================================

Define a subtle motion language.

Use animation for:
- Mini Player → Now Playing
- Play/pause state
- Track changes
- Bottom sheets
- Queue reordering
- Search/filter transitions

Avoid animation for:
- Every button press
- Simple navigation where it slows the user
- Large decorative effects

Animations must communicate state changes rather than exist purely for decoration.

==================================================
ACCESSIBILITY
==================================================

Design for:
- Large text
- Screen readers
- High contrast
- Color-blind users
- Large touch targets

Never rely on color alone to communicate:
- Playing state
- Selected state
- Errors
- Favorites

==================================================
NAVIGATION STRUCTURE
==================================================

Define the navigation hierarchy:

Home
 ├── Recently Played
 ├── Recently Added
 ├── Most Played
 ├── Albums
 ├── Artists
 └── Playlists

Library
 ├── Songs
 ├── Albums
 ├── Artists
 ├── Genres
 └── Folders

Search
 ├── Songs
 ├── Albums
 ├── Artists
 ├── Playlists
 └── Genres

Playlists
 ├── All Playlists
 └── Playlist Details

Settings
 ├── Playback
 ├── Library
 ├── Appearance
 ├── Audio
 └── About

Global:
 ├── Now Playing
 ├── Queue
 └── File Information

==================================================
EMPTY STATES
==================================================

Define the visual language for:
- No music
- No playlists
- No search results
- Empty queue
- Missing artwork

Empty states should be helpful without becoming oversized illustrations.

==================================================
DESIGN DELIVERABLE
==================================================

Create a reusable design system and representative screens demonstrating the system.

The result must be detailed enough that a developer can implement the interface consistently in Jetpack Compose.

Prioritize consistency over quantity.

This is the FOUNDATION of the product.

Do not invent unrelated features.

Do not add subscriptions, ads, accounts, social features, cloud music or recommendation feeds.

The final design should communicate:

OWN YOUR MUSIC.
PLAY IT FAST.
KEEP IT SIMPLE.