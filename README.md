# Wash It Clean 🧽💦

A relaxing hyper-casual Android game: drag your finger to pressure-wash muddy cars,
sneakers, patios, houses and boats until they sparkle. You earn coins for each level
and spend them on stronger nozzles. The game makes money from Google AdMob ads.

## How the game works

| Part | What it does |
|---|---|
| Levels | Generated from the level number, so they never run out. Dirt gets thicker and rewards grow as you go. |
| Washing | Dirt is stored as a 96×128 grid. The spray removes dirt with a soft round falloff. A level is done at 97% clean. |
| Shop | 5 nozzles, from the free Garden Hose to the Pro Blaster, each bigger and stronger. |
| Ads | **Rewarded** ads: ×3 coins after a level, or +50 coins in the shop. **Interstitial** ads: at most one every 3 levels and 90 seconds, never in a player's first 3 levels. **Banner**: main menu only. |
| Consent | Google's UMP consent form (required in the EEA/UK), with a "Privacy settings" button when needed. |

## Project layout

```
core/   Pure-Kotlin game logic (dirt grid, levels, shop, ad pacing) with unit tests.
        A separate Gradle build, so it builds and tests without the Android SDK.
app/    The Android app: screens, the WashView play area, AdMob and consent, saving progress.
.github/workflows/ci.yml       Tests, lint and builds on every push and pull request.
.github/workflows/release.yml  Builds a signed bundle and uploads it to Google Play.
docs/   Play Store release guide and a privacy policy template.
```

## Build and test

Requires JDK 17+ and, for the app, the Android SDK (Android Studio installs it).

```bash
./gradlew coreTest              # game logic unit tests (no Android SDK needed)
./gradlew :app:lintDebug        # Android lint
./gradlew :app:assembleDebug    # debug APK in app/build/outputs/apk/debug/
./gradlew :app:installDebug     # install on a connected phone or emulator
```

Debug builds always use Google's **test ad IDs**, so you can click the ads safely.
Never click real ads on your own app: AdMob can ban your account for it.

## Releasing to Google Play

See **[docs/PLAY_STORE_RELEASE.md](docs/PLAY_STORE_RELEASE.md)**. In short: create the
AdMob ad units and the Play Console app, add the signing key and IDs as GitHub secrets,
then push a tag such as `v1.0.0`.

## Ideas for next versions

- Before/after replay clip with a share button (good for TikTok/Shorts growth)
- Sound effects and music
- More object kinds and themed worlds
- Daily challenge object
- "Remove ads" in-app purchase (Google Play Billing)
