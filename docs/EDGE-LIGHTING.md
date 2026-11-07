# Meloqis Edge lighting — 1.8.0

Open **Settings → Edge lighting**. This is a separate full-screen destination. Equalizer remains under Audio and contains only sound controls. A first-use walkthrough explains each feature; Guide at the top reopens it.

## Create a look

Design offers Aurora frame, Neon pulse, Rainbow trail and Midnight clock presets. Choose Ribbon, Comet, Pulse, Bars, Wave, Orbit or Dots. Palettes include Aurora, Sunset, Ocean, Rainbow, Neon, Album art and a custom two-hue gradient. Adjust width, glow, motion speed, audio sensitivity, spectrum reach and mirroring. Album art uses the current cover through the existing image loader; Aurora is the fallback if it is unavailable. Artwork colours update with the track.

The preview is explicitly labelled Demo when no local audio is present. Demo does not play sound or publish fake audio into the player. Real displays use decoded Meloqis audio on this device. They do not capture other music apps, microphone input or Cast audio. Ambient mode supplies a gentle decorative glow between songs, not a synthetic music spectrum.

Fit places the lights around the rounded frame or along the top, bottom, left, right or center. Tune corner radius, width, edge distance and camera/notch gap. Android cutout rectangles are excluded on supported devices. The live overlay uses the full display's coordinates, rather than stretching the design into a small strip.

## Clock and screensaver

Clock offers Digital, Stacked, Analog and Minimal faces, a position control, local battery percentage, optional song details and brightness. Open clock display starts a user-visible Activity with a black background and an explicit Back control. It ends at the selected session limit or on screen-off. Text shifts periodically; this is a precaution, not a burn-in guarantee.

Set up Android screensaver opens Android's screensaver settings. Select **Meloqis Music Lights** and choose the charging/docked conditions supported by the phone. Android owns activation and wake/exit behavior. The service requires `BIND_DREAM_SERVICE`, is non-interactive (touch exits), dims the screen and caps requested brightness at 30%. It does not launch an Activity from the background, request accessibility access, dismiss the keyguard or replace the manufacturer's low-power hardware AOD. Device support varies.

## Floating lights

Show lights over other apps requests display-over-other-apps permission only when selected. The layer passes touches through and does not inspect other apps' content. Stop it in Edge lighting or from its ongoing notification. It stops on screen-off, removal from Recents, session expiry, permission revocation or battery saver when selected. With ambient glow disabled it also stops after 60 seconds without local audio. Sessions can be 15, 30, 60 or 120 minutes.

Preferences from 1.7 are retained, including placement, old visual modes and colours. New controls have bounded defaults. No microphone, notification-listener or usage-access permission is introduced. Festival controls remain outside the app's Settings panel.

## Verification

- Host tests exercise audio-envelope timing across frame rates, silence, decay, invalid input and attack/release behavior.
- The optional Android render probe draws the real native renderer to bitmaps without desktop UI actions. It checks 42 mode/placement combinations, four clocks, the quiet transparent overlay and notch controls, and restores the original lighting preferences afterward.
- Run `:androidApp:assembleDebugAndroidTest -PedgeRenderProbe=true` to build the probe. On a disposable test emulator, install the debug app and test APK, then run `adb shell am instrument -w echo.music.iad1tya.test/echo.music.iad1tya.test.EdgeLightingRenderProbe`. Default instrumentation settings are unchanged without the property.
- Bitmap outputs are renderer previews, not screenshots of the full app UI. They do not verify permission prompts, manufacturer-specific screensaver activation, touch-through behavior or GPU rendering on physical devices. Device testing remains necessary for those flows.
