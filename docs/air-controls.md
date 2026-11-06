# Meloqis Air Controls (beta)

Open **Air controls**, choose Scrolling or Volume, then tap **Enable camera**.
Camera permission is requested only here. First use downloads Google's 7.5 MB
hand detector; later sessions use the verified cached model. Open **Show gesture
guide** for step-by-step help. Keep the phone stationary in good light, wait for
**Air on**, then show one whole hand about 30–70 cm from the front camera.
Move your hand across the camera; wait a second between gestures.
With gestures enabled, reopen the guide to practice. It shows hand detection and
the last recognized swipe while pausing playback, volume and scrolling commands.

| Gesture | Action |
| --- | --- |
| Swipe right | Next queued track |
| Swipe left | Previous queued track |
| Swipe up, Scrolling mode | Scroll down |
| Swipe down, Scrolling mode | Scroll up |
| Swipe up, Volume mode | Raise music volume |
| Swipe down, Volume mode | Lower music volume |

Scrolling supports Home, Search (including results, history and moods), Library,
Library playlist grids, favorites/downloaded collections, and local/online playlists.
Other screens retain their touch controls. Tap **Air on → Stop camera**, **Stop
Air** in the full player, or **Stop camera** in the ongoing notification to stop.
By default, leaving the app stops the camera. Enable **Keep gestures active in
the background** before starting to keep next/previous and volume controls active
when using another app. Vertical swipes adjust volume in the background, even
if Scrolling is selected; scrolling applies only while Meloqis is visible. When
the full player is open, vertical swipes adjust volume rather than scrolling a
hidden page beneath it.
Swiping Meloqis away from recent apps stops the session. It never restarts after
a reboot or an Android process termination.

Only the front camera is used. MediaPipe hand landmark recognition gates commands:
without a detected hand there is no swipe action. Palm movement determines
direction; finger movement alone does not skip songs. Difficult lighting and
occluded hands can affect recognition. Manual controls remain available.

Frames are resized into a reusable 256 × 192 RGB buffer at no more than eight
analyses per second (roughly three while no hand is visible), using a low-resolution camera stream and CPU inference on
a worker thread. Frames are not saved, recorded or uploaded. The model is
downloaded from Google's pinned model URL and checked against its SHA-256 digest
before use. Recognition code and native libraries remain dormant unless enabled.
Camera hardware is optional; the app targets Android 8 (API 26) and newer.

Automated checks cover direction, cooldown, jitter, stale trajectories, absent
or malformed hand landmarks, and finger motion with a stationary palm. These do
not substitute for camera, orientation and performance tests on physical devices.
