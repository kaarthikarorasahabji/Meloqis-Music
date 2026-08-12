# Meloqis Music 0.1.13

- Enables the calibrated Meloqis Liquid Glass experience, floating navigation, haptics, and smarter queue defaults for existing and new listeners.
- Adds a one-time feedback prompt after 48 hours, plus a permanent Settings → Feedback & reports path for crashes, playback failures, update issues, and suggestions.
- Sends feedback privately through an Axenora-controlled Cloudflare Worker and Resend without embedding the email API key in the APK.
- Adds direct crash-screen reporting while keeping anonymous operational insights opt-in.
- Verifies downloaded updates with both SHA-256 and the installed Meloqis publisher certificate before installation.
- Fixes Now Capsule resource cleanup when playback or the app task closes, and improves accessibility actions for assistive technology.
- Removes duplicate widget receiver declarations and hardens transactional database reads.
