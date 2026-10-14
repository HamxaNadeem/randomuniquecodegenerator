# Releasing Blocky Gems on Google Play

The steps are the same as for Wash It Clean (see `docs/PLAY_STORE_RELEASE.md` at the
repository root). Only the names below differ, because each app needs its own AdMob
IDs and signing key.

## One-time setup
1. **AdMob:** add an Android app called "Blocky Gems" and create **Banner**,
   **Interstitial** and **Rewarded** ad units. Set up the GDPR message under
   **Privacy & messaging**.
2. **Signing key:**
   ```bash
   keytool -genkeypair -v -keystore blocky.jks -alias blockygems -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 blocky.jks > blocky.jks.b64
   ```
   Back up `blocky.jks` and its passwords. Never commit them.
3. **Play Console:** create the app with package name `com.hamxanadeem.blockygems`.
   Fill in the store listing, content rating, target audience (13+), **Data safety**
   (AdMob collects the advertising ID, approximate location, app interactions and
   diagnostics) and a privacy policy URL. A template is at `docs/PRIVACY_POLICY.md`
   in the repository root; replace the app name. **Upload the first bundle by hand**
   to Internal testing.
4. **Service account:** you can reuse the Wash It Clean one. In Play Console, give
   it **Release** permission for Blocky Gems too.
5. **GitHub secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `BLOCKY_KEYSTORE_BASE64` | contents of `blocky.jks.b64` |
| `BLOCKY_KEYSTORE_PASSWORD` | keystore password |
| `BLOCKY_KEY_ALIAS` | `blockygems` |
| `BLOCKY_KEY_PASSWORD` | key password |
| `BLOCKY_ADMOB_APP_ID` | AdMob app ID (`ca-app-pub-…~…`) |
| `BLOCKY_ADMOB_BANNER_ID` | banner unit ID |
| `BLOCKY_ADMOB_INTERSTITIAL_ID` | interstitial unit ID |
| `BLOCKY_ADMOB_REWARDED_ID` | rewarded unit ID |
| `PLAY_SERVICE_ACCOUNT_JSON` | service account JSON key (shared) |

## Releasing
```bash
git tag blocky-v1.0.0
git push origin blocky-v1.0.0
```
This uploads a signed bundle to the **internal** track as a **draft**. To pick a
track yourself, use **Actions → Release Blocky Gems to Google Play → Run workflow**.

## Store listing tips for more installs
- **Icon and first screenshot:** show a board mid-combo with the "Excellent!" cheer.
  Bright gems on the dark background stand out in search results.
- **Short description:** "Relaxing gem block puzzle: fill lines, chain combos, beat your best!"
- **Keywords to use naturally in the description:** block puzzle, jewel, gem, brain
  game, offline, relaxing, classic.
- **Promo video:** record 15–30 seconds of a big combo for the listing and for
  TikTok/Shorts.
