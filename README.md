# Pattern Fidget

A calm, tactile fidget app: a lock-screen style dot grid you draw on, with soft physics,
gentle generative music and a quiet haptic language. Nothing unlocks. Nothing is scored.

## What's in 4.0 (3.0 features, redesigned)
- **Home is the canvas.** Touch the grid and you're drawing; the interface fades away while
  your finger is down and returns 1.5 s after you lift. At rest the grid breathes and traces
  a faint pattern (or a favourite you choose).
- **Modes** (tune icon or the Draw button): Free Draw, Endless Flow, Constellation, Ripple,
  Mirror (vertical or four-way), Pattern Loop (drag up/down for speed, tap to clear) and Zen.
- **Zen**: long-press the home grid, or pick it from Modes. Only the grid. Two-finger tap to return.
- **Feel**: dots lean toward your finger and spring when captured, lines taper and trail like
  thread, ripples are wide and slow for slow strokes and tight for quick ones. Each shape has
  its own finish: loops glow around twice, zigzags ripple, straight lines pulse end to end,
  symmetric shapes glow from both ends, connecting every dot makes the grid exhale.
- **Sound**: pentatonic notes that follow your stroke direction (up rises, down falls, sideways
  shifts voice, diagonals add a harmony). Nine environments: Chime, Soft Synth, Glass, Water,
  Wood, Rain, Bells, Piano, Bubbles. All synthesized on the phone.
- **Haptics**: tick on entering, slightly stronger with each dot, a soft double pulse on finish,
  a slow swell for every dot, a muted double tick for a missed Memory pattern. Light/Medium/Strong.
- **Memory**: watch, then echo. Relaxed (faint guide stays), Classic, Focus (plays once).
- **Journey** (card on Home): handcrafted levels in worlds (Dusk 3×3, Tide 4×4). Five puzzle types:
  Trace an outline, Path (touch gold dots, avoid crossed ones), Silhouette, Reverse and Mirror.
  1–3 stars for neatness, no timers or lives, skip after three tries. Settings → Hide scores.
  A miss offers Try again, Show pattern or Start fresh. No levels, no timers.
- **Collection**: living cards that redraw themselves. Favourites, names, grid filters,
  playback with speed and loop, and "Use as home animation".
- **Themes** set the whole experience: Dusk, Fog, Sage, Tide, Ink each have their own palette,
  glow, pace, musical key, sound and haptic character.
- **Comfort & accessibility**: Reduce motion (follows the system setting), high contrast,
  AMOLED true black, larger dots and touch areas, quiet completions, TalkBack support
  (each dot is a button; a "Finish pattern" control completes it), nothing needs sound or vibration.

Android 8.0+ (minSdk 26). No third-party libraries. Nothing leaves the device.

## Get the APK, option A: GitHub (no tools needed on your PC)
1. Upload everything in this folder to your repo (including the hidden `.github` folder), replacing the old files.
2. Open the repo's **Actions** tab. The "Build APK" workflow runs automatically (or press "Run workflow").
3. When it finishes (~3–5 min), open the run and download the **PatternFidget-apk** artifact (a zip containing `app-debug.apk`).

## Get the APK, option B: Android Studio
1. **File → Open** this folder and let Gradle sync.
2. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Install on your phone
Copy the APK to the phone and tap it. Allow "Install unknown apps" once when Android asks.
It installs over version 2 and keeps your saved patterns.

## Code map (4.0: Compose screens around the proven grid)
- `MainActivity.kt`, `ui/FidgetApp.kt`: the Compose host and Navigation graph (home, draw, draw/{mode}, zen, memory, collection, playback, settings, themes)
- `ui/theme/`: design tokens (colours, spacing, radii, motion, type) and `FidgetTheme`
- `ui/components/`: `PatternGrid` (hosts the grid), `GlowCard`, `FidgetBottomBar`, `ModeCard`, `ThemePreviewCard`, `PatternThumb`, controls and icons
- `ui/home|draw|memory|collection|settings|themes/`: one folder per screen
- `PatternView.kt`: the grid itself: touch, lock rules, modes, springs, layered glow, ripples, completions, accessibility
- `interaction/`: `GridFeedback` (grid events to sound, haptics, counter) and `SavePrompt`
- `domain/`: `MemoryGameEngine`, `levels/` (Journey levels, rules and par solver) and display formatting
- `data/FidgetEnv.kt`: observable settings, store, sound and haptics shared by every screen
- `Sound.kt` (ten synthesized sound packs, incl. Marimba), `Haptics.kt`, `Theme.kt`, `Shapes.kt`, `Patterns.kt`, `Prefs.kt`
