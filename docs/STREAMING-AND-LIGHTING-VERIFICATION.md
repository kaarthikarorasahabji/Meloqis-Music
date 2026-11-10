# Playback and lighting verification — 1.11.0

Validated on 10 November 2026:

- Debug app and custom instrumentation APK compile successfully.
- All 19 common Android host tests pass, including hidden lighting for locked, non-interactive, unfocused, background and disabled states.
- API 33 x86_64 emulator: default-on lighting, persistent off, re-enable, focus loss/recovery using an application dialog, background stop, and absence of a system overlay window pass. Preferences are restored after the check.
- Real ExoPlayer with the production `PlaybackStreaming` cache factory and load control reads a throttled synthetic 60-second WAV. Playback starts in 499 ms with 10,156 of 960,044 bytes read. Releasing the player commits a nonempty playback cache.
- With a concurrent lock covering the playback cache entry, playback bypasses that lock and starts in 364 ms with 11,180 bytes read. This verifies nonblocking streaming rather than waiting for a downloader/cache writer.

The synthetic source avoids changing release network security or depending on an external music service. These timings are regression-test observations, not promised network startup times. The probe tests the production cache/buffering policy; it does not benchmark YouTube URL extraction, every decoder, phone, or connection.

Physical charging/lock behavior and visual appearance on different manufacturers still need device QA. No full-app screenshot review is claimed. The app has no DreamService, floating lighting service, overlay permission, lighting wake lock or charging receiver. Both keyguard and device-lock state are checked before attaching the activity-local layer.

Incoming playback bytes populate the bounded playback cache. This is separate from a permanent offline Download; it does not promise that an entire unplayed queue will be downloaded.

To rerun the device checks, build the debug app and Android test APK using `-PstreamingProbe=true`, install both, then run `echo.music.iad1tya.test.StreamingPlaybackProbe`. Rebuild the test APK with `-PinAppLightingProbe=true` for `InAppLightingProbe`. The runner selection only affects instrumentation; production APKs contain neither runner.
