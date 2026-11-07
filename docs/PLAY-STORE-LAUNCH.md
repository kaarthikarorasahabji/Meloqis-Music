# Meloqis Music launch

This package prepares a first Google Play upload. Building an AAB does not publish the app or guarantee acceptance. The app's public pages are configured for `https://axenoraai.in/meloqis/`; they must be deployed before submission.

## Files

- `launch-package/Meloqis-Music-play-release.aab`: signed Android App Bundle for Play Console. An AAB cannot be installed directly on a phone.
- `launch-package/mapping.txt`: R8 deobfuscation mapping for this exact build.
- `launch-package/upload-certificate.pem`: public upload certificate.
- `launch-package/SHA256.txt`: bundle checksum.
- `launch-package/Meloqis-hosting.zip`: public pages and assets; extract into the website root to create `/meloqis/`.
- `release-private/meloqis-upload.jks` and `release-private/signing.json`: private upload key and passwords. Back up securely. Never upload these to the site or commit them.

## Build another release

Install JDK 21 and the Android SDK required by the project (compile SDK 37, target SDK 36). Ensure `androidApp/google-services.json` belongs to a Firebase project you control and registers `echo.music.iad1tya`.

Run in PowerShell:

```powershell
./scripts/build-play-release.ps1 -AndroidSdk 'D:/Android/Sdk'
./scripts/package-hosting.ps1
```

If needed, pass `-JavaHome` to the build script with the JDK 21 installation path. The script reads the private signing file, builds with `-PplayStore=true`, verifies the bundle signature and copies the AAB, mapping and checksum to `launch-package`. It disables Crashlytics mapping upload during the local build; configure Firebase access separately if using automated mapping uploads. Build outputs can still contain environment-specific configuration; review Firebase ownership before distribution.

The Play build removes `REQUEST_INSTALL_PACKAGES` and advertising ID permission, disables advertising ID collection and personalization signals, and directs update actions to Google Play. Regular sideload builds keep their existing updater. Privacy/support links are in Settings → About us.

## Hosting at axenoraai.in

1. Run the hosting packaging script.
2. Extract `Meloqis-hosting.zip` into the existing website's public root. Keep the containing `meloqis` folder. On a framework site, copy it into that site's public/static directory, or route `/meloqis/` to the static files. Do not overwrite your site's homepage.
3. Deploy the existing site through its hosting provider with HTTPS. These public pages need only static hosting. The separate owner-private Meloqis Studio handles push campaigns with its deployed server and database; music providers, lyrics, Firebase and optional integrations remain external dependencies.
4. Verify these pages return HTTP 200 without login, geographical restrictions or download prompts:
   - `https://axenoraai.in/meloqis/`
   - `https://axenoraai.in/meloqis/privacy.html`
   - `https://axenoraai.in/meloqis/support.html`
   - `https://axenoraai.in/meloqis/delete-data.html`
5. Use the privacy URL and support email `kaarthikdassarorasahabji@gmail.com` in Play Console. `_headers` and `vercel.json` contain optional provider security headers; apply equivalent settings to your existing host as appropriate. These files are scoped to the static package and do not configure routing in your existing website.

The download button currently points to existing GitHub APK releases. Switch it to the verified Play listing after Google approves the app. Do not invent a live listing or replace the APK button before it exists.

## Play Console submission

1. Create the app in your verified Play developer account. Choose Music & Audio, appropriate countries, audience and pricing. Confirm package ID `echo.music.iad1tya` is available to your account: it is inherited from the upstream app, and Play package names are globally unique. If unavailable, choose a unique package, update Firebase and all app/store links, then rebuild before the first upload.
2. Enroll in Play App Signing; allow Google to manage the app signing key. The generated local key signs uploads. It is a different key from previously distributed sideload builds; those installations may need to be removed before installing a Play-signed copy. Export libraries first.
3. Upload the AAB to internal testing and inspect Play's bundle analysis, permissions, native-library compatibility and pre-launch report. Attach the exact mapping file if it is not detected automatically.
4. Complete Data safety, privacy policy, ads, content rating, target audience, app access instructions and foreground service declarations. Explain floating Music Lights, music playback and user-requested downloads with the required evidence. Never provide personal login credentials to reviewers; use a dedicated test account if necessary.
5. Add a 512 × 512 store icon, 1024 × 500 feature graphic and current phone screenshots. Existing screenshots may show older versions; capture the exact build to avoid misleading users. Listing copy is supplied in `docs/STORE-LISTING.md`.
6. Complete any testing and production access requirements shown for your account before requesting production rollout.

## Issues that still need owner review

- **Music rights and third-party access:** the source describes YouTube extraction, ad-free/background streaming and media downloading. Confirm rights and provider authorization for the actual catalog and behavior before submitting. An open-source license or disclaimer does not grant music rights. If necessary, change the product to authorized sources or local playback; packaging alone does not resolve this.
- **Firebase ownership and declarations:** the checked-in configuration points to Firebase project `empmetria`. Confirm that you control this project, its retention and SDK data practices; otherwise replace the configuration with your own Firebase project and rebuild. Do not claim “no data collected”: Analytics and Crashlytics are included. The privacy pages describe current integrations and must be reviewed against the actual project settings and launch behavior.
- **Native libraries:** confirm every shipped 64-bit native dependency supports 16 KB page sizes. Passing a bundle build alone is insufficient. Resolve any verification-report failures before submission and test on a 16 KB device or emulator.
- **API integrations:** Last.fm credentials are blank unless provided through local configuration; optional AI features require user-configured credentials. Verify all features you advertise. Do not bundle private server credentials inside the app.
- **Device QA:** test first run, account sign-in, streaming, lyrics, downloads, local audio permission, notifications, Music Lights permission denial and overlay start/stop behavior, casting and upgrade recovery. A successful compiler check cannot replace this.
- **Source distribution:** the hosting package includes `meloqis/source/Meloqis-Music-source.zip`, a snapshot of the current build sources including local changes and the Gradle wrapper. Publish it alongside this release and retain the required notices. Regenerate the hosting package after any source changes. Existing GitHub releases do not automatically contain these local changes.

## Official references

- [App signing](https://developer.android.com/studio/publish/app-signing)
- [Upload an app bundle](https://developer.android.com/studio/publish/upload-bundle)
- [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)
- [Intellectual property](https://support.google.com/googleplay/android-developer/answer/9888072)
- [Install packages permission](https://support.google.com/googleplay/android-developer/answer/12085295)
