# Meloqis Music

Meloqis Music is an independent Android music player maintained by Kaarthik Dass
Arora through [Axenora AI](https://axenoraai.in). The public home of the product is
[meloqis.axenoraai.in](https://meloqis.axenoraai.in).

> Development status: pre-release (`0.1.0`). Do not publish an APK until the
> release checklist in [MELOQIS_RELEASE_CHECKLIST.md](MELOQIS_RELEASE_CHECKLIST.md)
> is complete.

## Meloqis Now Capsule

Meloqis Now Capsule is an optional animated playback overlay for Android. It:

- works on Android 13, including a Samsung Galaxy S21 Ultra;
- is disabled by default;
- asks for Android's **Display over other apps** permission only when enabled;
- displays and controls only Meloqis playback;
- does not read notifications, accessibility data, or activity from other apps.

Enable it from **Settings → Player → Meloqis Now Capsule**. Long-press the
capsule to return to the app.

## Build identity

- App name: `Meloqis Music`
- Application ID: `in.axenoraai.meloqis`
- Website: `https://meloqis.axenoraai.in`
- Official/company website: `https://axenoraai.in`
- Custom link scheme: `meloqis://`
- Maintainer: `Kaarthik Dass Arora`
- Instagram: `https://www.instagram.com/kaarthikarora`
- LinkedIn: `https://www.linkedin.com/in/kaarthikdassarora`

The legacy Kotlin source namespace is temporarily retained to keep the first
fork migration reviewable. It does not determine the installed Android package.

## Building

Use JDK 21 and the included Gradle wrapper:

```powershell
.\gradlew.bat assembleUniversalFossDebug testUniversalFossDebugUnitTest
```

Private service configuration belongs in `local.properties` or CI secrets and
must never be committed. OAuth client secrets must not be compiled into an APK.

## Licensing and attribution

Meloqis Music is a modified fork of
[Echo Music](https://github.com/EchoMusicApp/Echo-Music), originally distributed
under the GNU General Public License version 3. Meloqis Music remains licensed
under GPLv3. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).

Distributing an APK requires providing the exact corresponding source for that
APK, including build scripts and Meloqis modifications. Branding Meloqis as an
independent fork does not permit removing the original copyright history.

The software license does not grant rights to music, cover art, lyrics,
trademarks, or third-party services. A public release must use local/user-owned
media or properly licensed catalog and service integrations.

---

Developed with ❤️ by Kaarthik Dass Arora
