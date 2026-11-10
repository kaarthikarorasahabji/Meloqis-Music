# Playback and lighting verification — 1.11.0

Validated on 10 November 2026:

- Debug app and custom instrumentation APK compile successfully.
- All 22 Android host tests pass (19 common tests and three concurrency regression tests), including hidden lighting for locked, non-interactive, unfocused, background and disabled states.
- API 33 x86_64 emulator: default-on lighting, persistent off, re-enable, focus loss/recovery using an application dialog, background stop, and absence of a system overlay window pass. Preferences are restored after the check.
- Real ExoPlayer with the production `PlaybackStreaming` cache factory and load control reads a throttled synthetic 60-second WAV. Final playback starts in 562 ms with 11,244 of 960,044 bytes read. Releasing the player commits a nonempty playback cache.
- With a concurrent lock covering the playback cache entry, playback bypasses that lock and starts in 362 ms with 10,668 bytes read. This verifies nonblocking streaming rather than waiting for a downloader/cache writer.

The synthetic source avoids changing release network security or depending on an external music service. These timings are regression-test observations, not promised network startup times. The probe tests the production cache/buffering policy; it does not benchmark YouTube URL extraction, every decoder, phone, or connection.

Physical charging/lock behavior and visual appearance on different manufacturers still need device QA. The app was operated visually on the emulator for real-song checks; this is not a visual review across devices. The app has no DreamService, floating lighting service, overlay permission, lighting wake lock or charging receiver. Both keyguard and device-lock state are checked before attaching the activity-local layer.

Incoming playback bytes populate the bounded playback cache. This is separate from a permanent offline Download; it does not promise that an entire unplayed queue will be downloaded.

To rerun the device checks, build the debug app and Android test APK using `-PstreamingProbe=true`, install both, then run `echo.music.iad1tya.test.StreamingPlaybackProbe`. Rebuild the test APK with `-PinAppLightingProbe=true` for `InAppLightingProbe`. The runner selection only affects instrumentation; production APKs contain neither runner.

## Real network playback checks

On the API 33 emulator, before the direct resolver change, Khat (`KrJ5c-Egz-U`) took about 10.6 seconds from player creation to READY. KALYANI Remix (`zAiIgYOH4Ys`) took about 9.2 seconds. Traces show PipePipe failing a bot-check, followed by Brave extraction and a URL HEAD check. These were fresh resolutions, not complete-file downloads.

The direct audio path fetches the existing extractor library's successful VisionOS player response in parallel with music metadata. It avoids watch-page/optional metadata extraction and validates through the actual media GET. The existing error retry invalidates the format and puts that direct route on a five-minute per-track cooldown so it can use the full fallback.

Observed real-song READY timings (player creation to first STATE_READY, using the existing precache trigger as the listener marker):

- Gehra Hua (`i1o1p_DD6TU`), fresh direct resolution before bounded preparation: 3.335 seconds.
- Runaway — AURORA (`BRpS93I8_Jk`), new Home pick resolved and header/index prepared before tap: 1.133 seconds.
- Believer — Imagine Dragons (`Kx7B-XvmFtE`), top search suggestion prepared before tap: 0.311 seconds. Preparation completed at 09:59:21.620 IST; player creation was 09:59:46.647 and READY was 09:59:46.958. Its URI resolution reused the stored format.

These observations involve different tracks and network conditions, not a controlled same-track speedup percentage. Fresh, unprepared tracks can still take seconds. No promise of zero-delay playback or equivalence to YouTube Music across every song/connection is made. Playback was verified by readiness, decoded playback state and visible progress; audible output on a physical phone remains device QA.

## Bounded preparation

Only the visible Home pick or top playable search suggestion/result is prepared after a short debounce. Data Saver skips preparation. Active lookups are shared; cancelling a preparation waiter does not cancel the selected track's lookup. Failed lookups can be retried, and unrelated tracks do not wait behind one another (three host regression tests).

Preparation reads at most 64 KiB at the start and 64 KiB at the index tail. A tail request requires a Content-Range response from the header request, avoiding a potentially full-file skip when a server ignores Range. Large tracks are not fully downloaded by this feature. Reading ranges still fills the normal bounded playback cache.

The final native streaming probe passes all four cases: unprimed streaming/cache, a locked cache bypass, prepared playback, and a server without range support. Prepared synthetic playback starts in 51 ms after 131,692/960,044 bytes read (including preparation and subsequent reads); the preparation itself stays within 128 KiB and commits the tail. The no-range case only prepares the 64 KiB header. These are controlled regression timings, separate from the real-song observations above.
