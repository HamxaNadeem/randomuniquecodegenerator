# Releasing Wash It Clean on Google Play

Do the one-time setup once. After that, each release is just pushing a git tag.

## One-time setup

### 1. AdMob (how the game earns money)
1. Sign up at <https://admob.google.com> and add an Android app called "Wash It Clean".
   You can link it to the Play Store listing later.
2. Create three ad units: **Banner**, **Interstitial** and **Rewarded**.
3. Note the **App ID** (`ca-app-pub-…~…`) and the three **ad unit IDs** (`ca-app-pub-…/…`).
4. In AdMob, go to **Privacy & messaging** and create a **GDPR** message. The app's consent
   form shows it to users in the EEA and UK.
5. After the app is live, add an `app-ads.txt` file to the developer website listed in
   Play Console. AdMob shows you the exact line to put in it.

### 2. Upload (signing) key
Create it once and back it up somewhere safe. If you lose it, you have to ask Google
to reset the upload key.

```bash
keytool -genkeypair -v -keystore release.jks -alias washitclean \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks > release.jks.b64   # macOS: base64 -i release.jks -o release.jks.b64
```

### 3. Google Play Console
1. Create a developer account at <https://play.google.com/console>. There is a one-time
   $25 fee, and new personal accounts must run a closed test with testers before going
   to production.
2. Create the app with package name `com.hamxanadeem.washitclean`.
3. Fill in the store listing, content rating questionnaire and target audience. Do not
   target children unless you also set up AdMob for child-directed treatment.
4. Fill in **Data safety**. The AdMob SDK collects the device advertising ID, approximate
   location (IP), app interactions and diagnostics, for advertising and analytics.
   Select **Contains ads**.
5. Publish a privacy policy (template: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)) at a public
   URL and add the link in Play Console.
6. **Upload the first bundle by hand.** The Play API can't create the first release of a
   new app. Build it locally with the signing env vars below (or run the release workflow
   once and download the `.aab` artifact), then upload it to the **Internal testing** track.

### 4. Service account (lets GitHub upload releases)
1. In Google Cloud Console, create a service account in any project and enable the
   **Google Play Android Developer API**.
2. Create a JSON key for that service account.
3. In Play Console, go to **Users and permissions**, invite the service account's email
   and give it **Release** permissions for this app.

### 5. GitHub secrets
In the repository, go to **Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | contents of `release.jks.b64` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `washitclean` (or the alias you chose) |
| `KEY_PASSWORD` | key password |
| `ADMOB_APP_ID` | AdMob app ID |
| `ADMOB_BANNER_ID` | banner ad unit ID |
| `ADMOB_INTERSTITIAL_ID` | interstitial ad unit ID |
| `ADMOB_REWARDED_ID` | rewarded ad unit ID |
| `PLAY_SERVICE_ACCOUNT_JSON` | the whole service account JSON key |

## Releasing a new version

```bash
git tag v1.0.0
git push origin v1.0.0
```

The **Release to Google Play** workflow runs the tests and lint, builds a signed,
shrunk bundle with your real ad IDs, and uploads it to the **internal** track as a
**draft**. Open Play Console to review it and roll it out.

To choose a track yourself, open **Actions → Release to Google Play → Run workflow**
and pick `internal`, `alpha`, `beta` or `production`, with status `draft` or `completed`.

## Building a signed release locally

```bash
export KEYSTORE_PATH=/path/to/release.jks KEYSTORE_PASSWORD=… KEY_ALIAS=washitclean KEY_PASSWORD=…
export ADMOB_APP_ID=… ADMOB_BANNER_ID=… ADMOB_INTERSTITIAL_ID=… ADMOB_REWARDED_ID=…
./gradlew :app:bundleRelease   # app/build/outputs/bundle/release/app-release.aab
```

If the `ADMOB_*` values are missing, the build falls back to Google's test ads.
The release workflow refuses to run without them, so a store build never ships test ads.
