# Pattern Fidget

A fidget app: a lock-screen style dot grid you draw patterns on, with haptic ticks,
rising tones, and soft ripple animations. Nothing gets unlocked; it's just satisfying.

- **Landing page** that quietly draws patterns on its own, with your lifetime dot count
- **Free draw**: draw anything; save the ones you like
- **Memory**: watch a pattern, draw it back; each level adds a dot. Best level is kept per grid size
- **Saved patterns**: thumbnails of everything you saved; tap one to replay it with sound
- **Sound packs**: Chime, Piano, Marimba, Bubbles (all synthesized on the phone, no audio files)
- 3×3 / 4×4 / 5×5 grids, 5 calm themes (Dusk, Fog, Sage, Tide, Ink), sound and vibration toggles

Android 8.0+ (minSdk 26). No third-party libraries.

## Get the APK, option A: GitHub (no tools needed on your PC)
1. Create a new GitHub repo and upload everything in this folder (including the hidden `.github` folder).
2. Open the repo's **Actions** tab. The "Build APK" workflow runs automatically (or press "Run workflow").
3. When it finishes (~3–5 min), open the run and download the **PatternFidget-apk** artifact (a zip containing `app-debug.apk`).

## Get the APK, option B: Android Studio
1. Install Android Studio, then **File → Open** this folder and let Gradle sync.
2. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

Or from a terminal with the Android SDK installed: `./gradlew assembleDebug`

## Install on your phone
Copy the APK to the phone and tap it. Android will ask you to allow
"Install unknown apps" for your file manager or browser; allow it once.

## Code map
- `MainActivity.kt`: all screens (landing, free draw, memory, saved, replay, settings) and navigation
- `PatternView.kt`: the grid, touch tracking, lock-style rules, playback, drawing & animations
- `Feedback.kt`: vibration, sound packs, success/miss sounds (synthesized at first launch)
- `Patterns.kt`: random drawable patterns for Memory, saved-pattern storage
- `Widgets.kt`: pattern thumbnails, back icon, theme swatches
- `Theme.kt`: color themes
