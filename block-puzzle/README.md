# Blocky Gems 💎

A glossy block puzzle game for Android. Drag gem pieces onto an 8×8 board and fill
rows or columns to make them burst. Clear lines back-to-back for combos and chase your
best score. It needs no internet to play, and each round lasts a few minutes. The game
is made for the "one more go" feeling that makes block puzzles some of the most
installed games on Google Play.

## What makes it fun to play
- **Jewel graphics:** bevelled, glossy gem blocks in 7 colours; an animated night sky
  with glowing orbs; a bobbing gem logo.
- **Juicy feedback:**
  - lines about to clear light up in your piece's colour while you drag
  - cleared cells burst in a wave with particles
  - lines flash and the screen shakes on big clears
  - "+points" floats up and the score counts up
  - cheers ("Great!", "Excellent!", "Amazing!", "Unbelievable!", "All Clear!") and "Combo x3"
  - new pieces pop into the tray
- **Fair dealing:** pieces that can't fit are never dealt while any piece could. A
  piece that completes a line is often slipped in, which keeps runs going.
- **Sound and touch:** bell-like sound effects synthesised in code (no audio files)
  and haptic feedback. Both can be turned off on the home screen.
- **Never lose progress:** an unfinished game is saved automatically, and the home
  screen shows "Continue".
- **Fast:** one custom view, cached gem bitmaps and allocation-free particles keep it
  at the screen's full frame rate, even on cheap phones. The APK has no image or
  sound files.

## Ads (Google AdMob)
| Type | Where |
|---|---|
| Rewarded | **Revive** after game over: clears the 3 fullest rows and deals fresh pieces, once per game. Players choose to watch, so it is the best earner. |
| Interstitial | Between games only, at most every 2 games and 2 minutes, never in a player's first 2 games. |
| Banner | Home screen only, never during play. |

The EEA/UK consent form (Google UMP) is included, with a "Privacy settings" button when needed.

## Build and test
From the repository root:

```bash
./gradlew -p block-puzzle coreTest            # game rules tests (no Android SDK needed)
./gradlew -p block-puzzle :app:lintDebug
./gradlew -p block-puzzle :app:assembleDebug  # APK in block-puzzle/app/build/outputs/apk/debug/
./gradlew -p block-puzzle :app:installDebug   # install on a connected phone
```

Debug builds use Google's test ads, so tapping them is safe.

## Project layout
```
core/  Pure-Kotlin rules: board, 37 piece shapes, fair piece dealing, scoring and
       combos, save/resume, ad pacing, sound synthesis. Unit tested.
app/   Android app: GameView (board, drag and drop, effects), gem renderer,
       animated background, home and game screens, AdMob and consent, sound.
docs/  Play Store release guide.
```

## Releasing
See [docs/PLAY_STORE_RELEASE.md](docs/PLAY_STORE_RELEASE.md). In short, add the
`BLOCKY_*` secrets, then push a tag such as `blocky-v1.0.0`.
