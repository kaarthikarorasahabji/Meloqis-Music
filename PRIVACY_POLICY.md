# Meloqis Music Privacy Policy

Last updated: 2026-08-12

This draft applies to Meloqis Music, operated by Kaarthik Dass Arora through
Axenora AI. It must be reviewed and completed with the operator's legal address,
support email, production services, and retention periods before public release.

## Data on the device

Meloqis may store settings, listening history, playlists, cached media,
downloads, account identifiers, and integration tokens on the device. Android
cloud backup is disabled for the Meloqis app. Clearing app data or uninstalling
the app removes app-private local data, subject to files the user exported
elsewhere.

## Optional permissions

- **Display over other apps:** used only when the user enables Meloqis Now
  Capsule. The capsule displays and controls Meloqis playback only.
- **Microphone:** used only for music-recognition features initiated by the user.
- **Notifications:** used for media playback and related app activity.
- **Media/audio access:** used to find or play audio selected by the user.
- **Nearby/Bluetooth:** used for supported audio-device behavior.

Meloqis Now Capsule does not require notification access or an accessibility
service and does not inspect other apps.

## Network services and integrations

When a user chooses a network feature, the app may send requests to the selected
provider. Examples in the source include music, artwork, lyrics, recognition,
Discord, Spotify, Last.fm, ListenBrainz, and casting services. Those providers
receive network information such as IP address and the data required for the
request, and their own privacy policies apply.

Only services explicitly enabled and rights-cleared by the Meloqis operator may
be included in a public release. Upstream credentials and services are not
Meloqis services.

## Accounts and tokens

Supported sign-ins must use the provider's official authorization flow. Tokens
are used only to perform actions requested through that integration. Meloqis
does not intentionally collect passwords. Production releases must use secure
token storage and an Axenora-controlled backend where a confidential OAuth
client is required.

## Anonymous app insights

Meloqis 0.1.8 and later can send limited operational events to an
Axenora-controlled Cloudflare Worker and D1 database. Anonymous insights are
disabled by default and can be enabled or disabled at any time in **Settings > Privacy >
Share anonymous app insights**.

The app generates a random installation UUID. It does not use an IMEI, phone
number, advertising ID, Android ID, account identifier, song title, artist,
playlist, listening history, precise location, or contact data for analytics.
The server immediately stores only a SHA-256 hash of the random UUID.

The following fields may be sent:

- first open and a maximum daily active signal;
- Meloqis version and version code;
- Android version and SDK level;
- update download, checksum, readiness, and confirmed-install outcomes;
- crash class and top application method, without a full stack trace; and
- Media3 playback error codes, without media identifiers or titles.

Cloudflare necessarily processes normal connection data such as an IP address
to deliver and protect the service. Meloqis does not write raw IP addresses to
its analytics database. For abuse prevention only, the service creates a
non-reversible keyed hash of the connection address and time window. These
short-lived rate-limit records are deleted within 48 hours. Detailed
operational events are automatically deleted after 90 days. Anonymous
installation activity and download counts are deleted after 400 days of
inactivity or collection.

Disabling anonymous insights stops future uploads and removes queued,
not-yet-uploaded events from the device. Previously aggregated counts cannot be
linked back to a person. Clearing Meloqis app data creates a new random
installation UUID if insights are later enabled.

## Feedback and reports

After 48 hours from first use, Meloqis may show a one-time feedback prompt. If
the user successfully submits feedback, that automatic prompt is not shown
again. The manual **Settings > Feedback & reports** form remains available for
crashes, bugs, playback failures, update problems, and feature suggestions.

Submitting feedback sends the selected rating, category, written message,
Meloqis version, Android version, and SDK level to an Axenora-controlled
Cloudflare Worker. The Worker forwards the report by email to the Meloqis
operator using Resend. The app does not attach an account identity, advertising
ID, phone number, media title, playlist, or listening history. Users should not
include passwords or other sensitive information in the written message.

Cloudflare and Resend necessarily process normal connection and delivery data
under their respective privacy terms. Feedback requests are rate limited. The
Resend API key is stored as an encrypted Cloudflare Worker secret and is never
included in the Android app.

## Children, deletion, and contact

Age limits, regional rights, deletion request handling, and a dedicated privacy
contact must be finalized before a global consumer release. Meloqis 0.1.8
remains a development preview.

Official website: https://axenoraai.in

Meloqis product website: https://meloqis.axenoraai.in

Developed with ❤️ by Kaarthik Dass Arora
