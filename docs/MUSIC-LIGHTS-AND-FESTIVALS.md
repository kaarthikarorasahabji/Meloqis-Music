# Music Lights and festival wishes — 1.8.0

Music Lights now has its own **Settings → Edge lighting** destination, separate from Equalizer. See [Edge lighting guide](EDGE-LIGHTING.md) for presets, rounded edge effects, seven palettes, fit controls, clock faces, the Android screensaver and the in-app walkthrough.

The analyzer taps a read-only view of the decoded PCM buffer in the existing equalizer pipeline. It never advances or changes the player buffer. Analysis runs only while a visible display requests it and publishes up to 20 real FFT frames per second. Rendering interpolates those frames. There is no simulated music, microphone permission, audio recording, upload or system-wide audio capture. Cast playback does not supply local PCM and therefore does not drive the lights.

The user-opened clock display keeps the screen awake, shifts clock content and exits at the chosen 15–120 minute limit or on screen-off. The optional touch-through overlay stops on screen-off, task removal, session expiry, permission revocation and selected battery-saver limits. With ambient glow disabled it also stops after 60 seconds of audio inactivity. Android screensaver integration is available where supported, with user selection in system settings, a dim black background and a 30% brightness cap. It does not replace a manufacturer’s low-power hardware AOD. The floating special-use foreground service still requires Play review.

Festival notifications are opt-in. Firebase project `empmetria` has a separate Meloqis Android registration for `echo.music.iad1tya`. Only opted-in Meloqis installations subscribe to `meloqis-festivals-v1`; Trackzen subscriptions are separate. Private Firebase credentials stay in the owner-private Meloqis Studio server runtime and are never bundled in Android or source archives.

Studio sends data-only messages. The Android receiver validates dated greeting text, expiry and consent, persists the message, deduplicates its notification by campaign ID, and shows an animated greeting when the app is resumed or the notification is opened. A cancelled Studio campaign has no independent local notification alarm. Future campaigns can contain their greeting content without an app update. The dated greetings are not automatically repeated on the same Gregorian date in another year.

The initial wishes use the dates requested by the owner: 8 November 2026 (Diwali), 9 November (Vishwakarma Day), and 10 November (Bhai Dooj), at 04:00 Asia/Kolkata. Studio's daily task checks due messages. Automation startup, Firebase transport, connectivity, force-stop and Android power saving can delay or prevent receipt; Firebase acceptance is not proof of delivery on every device.

Release builds for existing users must keep the existing APK signing identity. GitHub CI verifies its SHA-256 certificate before the device-specific APKs are published. The separate Play upload key created for the first Play bundle must not replace the existing sideload update key.

The app Settings panel contains no festival section. Consent is requested through the optional first-launch invitation; Android app notification settings control delivery afterward.
