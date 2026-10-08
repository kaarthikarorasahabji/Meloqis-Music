# Meloqis performance and player update

## What this folder contains

Meloqis is a Kotlin music app with a Compose interface and a Media3 playback service. The active Android entry point is `androidApp`; `composeApp` holds screens and view models. `core/domain` defines playback contracts, `core/data` coordinates repositories and queues, and `core/media/media3` implements playback, crossfade, and Android media sessions. The old `app` folder is excluded from the active Gradle build.

Existing features include streaming, downloads, playlists, synchronized lyrics, equalizer, sleep timer, crossfade, Android Auto, and artwork-driven player colors. This update strengthens those features instead of adding another rendering framework.

## Changes

- Startup and search no longer block the UI while reading preferences. Home refreshes are combined; search suggestions are debounced and obsolete searches canceled.
- Artwork network clients are reused and closed with the screen. Animated background values are read while drawing rather than rebuilding the whole screen every frame.
- Native playing bars and download indicators replace the unavailable Compottie animation runtime. Press feedback uses a brief scale animation. Decorative effects stop in the background and reduce automatically on low-memory devices or system battery saver.
- Image cache memory is capped at 15%, disk artwork cache at 128 MB, and notification artwork at 512 pixels. The unsafe global 100 MB database cursor-window override is removed.
- Lyrics timestamps are parsed once and searched using binary search. Equalizer reset also resets the preamp.
- Queue additions are serialized. Add to Queue appends even in endless mode. Moving and removing a shuffled queue row uses its displayed position; duplicate tracks are tracked by occurrence. Appending does not reshuffle the existing order.
- Queue and playlist reorder gestures are separated from a song row's Play Next gesture. Dragged rows follow the finger in the drawing layer, cancel safely, and ignore headers and footers as targets.
- Playback service destruction always releases its media session and controller. This addresses the connected phone's recorded `Session ID must be unique` crash. Repeated foreground-notification loops are removed.
- A new violet/teal music monogram, exported to compact raster assets, replaces the adaptive launcher icon and startup mark. Startup branding fades out in roughly 960 ms, while the home feed loads behind it. The home title shows native equalizer bars during playback; player surfaces transition to the current song's artwork colors.
- A dismissible developer support dialog appears on the first eligible home startup and then at most once every seven days. Its timestamp is saved before displaying it. The coffee button directly opens a generic UPI request to `kaarthikdassarorasahabji@sbi` with INR 50 prefilled and the UPI `mam=50.00` minimum-amount parameter. Enforcement depends on the receiving UPI app; no transaction is confirmed automatically.

The home feed and Settings include a static “Developed with love by Kaarthik Dass Arora Sahab Ji” footer. Tapping it opens a dedicated developer page with the Meloqis mark, creator name, support action, website, and project source link. Startup explicitly shows “Developed by Kaarthik Dass Arora Sahab Ji”.

## Controls

- **Song lists:** hold a row and move it right to Play Next. Hold and release without moving to open Play Next / Add to Queue.
- **Queue:** hold a song and move vertically to reorder. Its menu also offers Play Next, Move Up, Move Down, and Delete. Play Next moves the existing queue entry after the current song.
- **Editable local playlists:** reorder mode reserves the hold-and-move gesture for playlist ordering.
- **Player:** press Previous/Next to change tracks; hold them to seek backward/forward. Shuffle and repeat retain their existing controls.
- **Lightweight effects:** app battery saver, system power saver, and devices with 3 GB RAM or less reduce decorative motion and glass effects.

## Builds and compatibility

The active Android modules retain `minSdk = 26` (Android 8.0). The `performance` variant uses release code/resource shrinking and a local development signature. It installs as **Meloqis Preview**, package `echo.music.iad1tya.performance`, alongside the existing app. It does not replace or migrate the user's main installation.

Build and host-test tasks:

```text
gradlew.bat :androidApp:assemblePerformance :composeApp:testAndroidHostTest
```

The host needs a valid JDK rather than the currently invalid system JAVA_HOME. On this Windows host, quoting `-Djdk.net.unixdomain.tmpdir=D:\music app\build` in JAVA_TOOL_OPTIONS avoids the long temporary-path failure in Kotlin's compiler connection.

Phone APK: `androidApp/build/outputs/apk/performance/androidApp-arm64-v8a-performance.apk`.

## Verification

See the device verification notes below for the final installation. Host tests cover lyric boundaries, reverse seeking, duplicate timestamps, first support prompt, seven-day gating, and clock changes.

Android 8 compatibility is checked through the build's minimum SDK and Android lint checks. The connected Samsung Note 20 Ultra runs Android 13; an Android 8 device was not available. No claim is made that every device or every service/network condition has been tested. Playback and search still depend on their upstream services.

## Final delivery verification (8 October 2026)

- Signed version 1.5.0 (version code 150), original package `echo.music.iad1tya`, minimum SDK 26, target SDK 36. The APK signing SHA-256 matches the published 1.4.0 APK: `24be20817821f8c532c6f7289d358d0460b9862c5e5ef627802ed92498aa904b`.
- Release assembly, release lint and all seven Android host tests passed. ARM64 is approximately 24.51 MiB; universal is 49.71 MiB.
- Preview testing on Samsung Note 20 Ultra / Android 13 observed search results, streamed playback, lyrics and changing artwork colors. Queue gestures in every mode and the final UPI handoff were not independently verified. The device disconnected before installing the final release, so final release installation on the phone is pending.
- The new generated music monogram is preserved in `assets/meloqis-logo-v3.png`, with its prompt beside it. App assets are downscaled; Android adaptive icons include safe-zone padding and a monochrome export.
- The website uses native CSS motion, pauses continuous animations while hidden, honors reduced motion, and has one persistent universal-download link. Desktop and phone layouts, FAQs and mood switching were inspected in the browser. The player image is approximately 190 KB WebP.
- The latest-download workflow copies the single versioned universal APK into the stable asset name `Meloqis-Music-latest.apk` when a release is published, keeping the website's latest-release download link valid.
