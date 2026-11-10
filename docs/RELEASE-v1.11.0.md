Meloqis Music 1.11.0 — Quieter screens, quicker playback

- Removed the large Edge lighting cards from Home and Settings. Lighting is a normal Settings option, with customization and the remembered on/off switch.
- Lighting requires an unlocked, interactive screen and a focused Meloqis activity. Losing focus, locking the phone or leaving the app removes the decorative layer; a charging wake cannot start it by itself.
- Audio no longer waits for optional BPM/key metadata or an extra URL-check request before opening the stream. Expired URL recovery remains enabled.
- Playback starts with a short buffer while incoming audio fills the playback cache. Reduced excessive read-ahead and delayed next-track preloading until the selected track has buffered.
- Removed full-file range parameters so the media player controls bounded streaming requests. Offline Download remains available separately.

Open Settings → Edge lighting to customize or turn it off. Equalizer remains for audio controls.

Required-update support continues for direct APK installations running 1.9.0 or later. Android asks you to approve installation. The first Play Store submission is still pending; Play minimum-version enforcement remains disabled until an approved release is available there.

Network and music-service response times still affect startup. Physical charging and visual checks across manufacturers remain necessary; automated checks do not cover every phone.
